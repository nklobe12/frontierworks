package com.lodestar.backpacks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** What fitted upgrades do while a backpack is carried (worn or anywhere in the inventory). */
public final class Upgrades {
    private Upgrades() {}

    public static final int COOK_TIME = 160;

    /** Backpacks the player carries that have contents, worn one first. */
    public static List<Contents> carried(ServerPlayer p) {
        List<Contents> out = new ArrayList<>();
        BackpackStore store = BackpackStore.get(p.server);
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(p.getItemBySlot(EquipmentSlot.CHEST));
        stacks.addAll(p.getInventory().items);
        stacks.add(p.getOffhandItem());
        for (ItemStack s : stacks) {
            if (!(s.getItem() instanceof BackpackItem bi)) continue;
            var id = BackpackItem.peekId(s);
            if (id == null || !store.exists(id)) continue;
            out.add(store.contents(id, bi.tier.slots()));
        }
        return out;
    }

    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.isSpectator()) return;
        long t = p.level().getGameTime();
        if (t % 4 != (p.getId() & 3)) return;
        for (Contents c : carried(p)) {
            if (c.has(UpgradeItem.Kind.MAGNET)) magnet(p);
            if (c.has(UpgradeItem.Kind.FEEDING) && t % 20 < 4) feed(p, c);
            if (c.has(UpgradeItem.Kind.REFILL)) refill(p, c);
            if (c.has(UpgradeItem.Kind.SMELTING)) smelt(p.level(), c, 4);
        }
    }

    /** Pulls loose items within six blocks to the player. */
    public static void magnet(Player p) {
        AABB box = p.getBoundingBox().inflate(6);
        for (ItemEntity ie : p.level().getEntitiesOfClass(ItemEntity.class, box, ie -> ie.isAlive() && !ie.hasPickUpDelay())) {
            if (ie.getPersistentData().getBoolean("PreventRemoteMovement")) continue;
            ie.setPos(p.getX(), p.getY() + 0.5, p.getZ());
            ie.setDeltaMovement(0, 0, 0);
        }
    }

    /** Eats the most filling safe food in the backpack when the player is hungry enough not to waste it. */
    public static void feed(ServerPlayer p, Contents c) {
        if (p.getFoodData().getFoodLevel() > 14) return;
        int best = -1, bestFood = 0;
        for (int i = 0; i < c.items.getSlots(); i++) {
            ItemStack s = c.items.getStackInSlot(i);
            FoodProperties f = s.getFoodProperties(p);
            if (f == null || !f.getEffects().isEmpty() && f.getEffects().stream().anyMatch(pr -> !pr.getFirst().getEffect().isBeneficial())) continue;
            if (f.getNutrition() > bestFood && f.getNutrition() <= 20 - p.getFoodData().getFoodLevel() + 4) {
                best = i;
                bestFood = f.getNutrition();
            }
        }
        if (best < 0) return;
        ItemStack one = c.items.extractItem(best, 1, false);
        ItemStack left = one.finishUsingItem(p.level(), p);
        if (!left.isEmpty() && left != one) {
            ItemStack rem = ItemHandlerHelper.insertItemStacked(c.items, left, false);
            if (!rem.isEmpty() && !p.addItem(rem)) p.drop(rem, false);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5f, 1f);
    }

    /** Tops up hotbar stacks that run below half from matching items in the backpack. */
    public static void refill(ServerPlayer p, Contents c) {
        for (int h = 0; h < 9; h++) {
            ItemStack hs = p.getInventory().getItem(h);
            if (hs.isEmpty() || !hs.isStackable() || hs.getCount() > hs.getMaxStackSize() / 2) continue;
            int want = hs.getMaxStackSize() - hs.getCount();
            for (int i = 0; i < c.items.getSlots() && want > 0; i++) {
                ItemStack s = c.items.getStackInSlot(i);
                if (!ItemStack.isSameItemSameTags(s, hs)) continue;
                ItemStack got = c.items.extractItem(i, want, false);
                hs.grow(got.getCount());
                want -= got.getCount();
            }
        }
    }

    /** Runs the smelting upgrade for {@code ticks} ticks: burns fuel from the fuel slot and cooks the input. */
    public static void smelt(Level level, Contents c, int ticks) {
        ItemStackHandler s = c.smelt;
        ItemStack in = s.getStackInSlot(0);
        Optional<SmeltingRecipe> r = in.isEmpty() ? Optional.empty()
                : level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new RecipeWrapper(new ItemStackHandler(net.minecraft.core.NonNullList.of(ItemStack.EMPTY, in.copyWithCount(1)))), level);
        ItemStack result = r.map(x -> x.getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
        boolean canCook = !result.isEmpty() && s.insertItem(2, result.copy(), true).isEmpty();
        boolean changed = false;
        if (c.burn <= 0 && canCook) {
            ItemStack fuel = s.getStackInSlot(1);
            int bt = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
            if (bt > 0) {
                ItemStack rem = fuel.getCraftingRemainingItem();
                s.extractItem(1, 1, false);
                if (!rem.isEmpty() && s.getStackInSlot(1).isEmpty()) s.setStackInSlot(1, rem);
                c.burn = c.burnMax = bt;
                changed = true;
            }
        }
        if (c.burn > 0) {
            c.burn = Math.max(0, c.burn - ticks);
            changed = true;
            if (canCook) {
                c.cook += ticks;
                if (c.cook >= COOK_TIME) {
                    c.cook = 0;
                    s.extractItem(0, 1, false);
                    s.insertItem(2, result.copy(), false);
                }
            } else c.cook = 0;
        } else if (c.cook > 0) {
            c.cook = Math.max(0, c.cook - ticks * 2);
            changed = true;
        }
        if (changed) c.changed();
    }

    /** Pickup upgrade: items of a kind the backpack already holds go straight into it. */
    public static void pickup(EntityItemPickupEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemEntity ie = e.getItem();
        ItemStack stack = ie.getItem();
        if (stack.isEmpty()) return;
        int before = stack.getCount();
        for (Contents c : carried(p)) {
            if (!c.has(UpgradeItem.Kind.PICKUP)) continue;
            boolean holds = false;
            for (int i = 0; i < c.items.getSlots() && !holds; i++) holds = ItemStack.isSameItemSameTags(c.items.getStackInSlot(i), stack);
            if (!holds) continue;
            stack = ItemHandlerHelper.insertItemStacked(c.items, stack, false);
            if (stack.isEmpty()) break;
        }
        if (stack.getCount() == before) return;
        p.take(ie, before - stack.getCount());
        if (stack.isEmpty()) {
            ie.discard();
            e.setCanceled(true);
        } else {
            ie.setItem(stack);
        }
    }
}
