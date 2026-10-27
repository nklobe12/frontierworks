package com.lodestar.lapidary;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cutting wheel: cuts rough gems into cabochon, step or brilliant gems (chosen with the buttons on the left of the
 * augment slots). Needs power and an abrasive; the abrasive and output augments raise the quality, harder cuts roll
 * lower but give stronger gems, and may shatter the stone into grit.
 */
public class CuttingWheelBE extends MachineBlockEntity {
    public static final int SLOT_IN = 1, SLOT_GRIT = 2;
    public static final int OUT_X = 92, OUT_Y = 26;
    public static final int CUT_X = 131;
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 38).slot(9, 59, MachineLayout.Role.BATTERY)
            .slot(38, 21, MachineLayout.Role.INPUT).slot(38, 51, MachineLayout.Role.INPUT)
            .arrow(62, 35)
            .grid(OUT_X, OUT_Y, 2, 2, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .extra(4).build();

    private static final TagKey<Item> DIAMOND_DUST = ItemTags.create(new ResourceLocation("forge", "dusts/diamond"));

    /** Abrasives: quality bonus, cuts per item, shatter reduction. */
    public enum Abrasive {
        NONE(0, 0, 0f), EMERY(0, 2, 0f), GRIT(8, 4, 0.03f), DIAMOND(18, 8, 0.06f);

        public final int bonus, uses;
        public final float steady;

        Abrasive(int bonus, int uses, float steady) {
            this.bonus = bonus;
            this.uses = uses;
            this.steady = steady;
        }

        public static Abrasive of(ItemStack st) {
            if (st.isEmpty()) return NONE;
            if (st.is(Content.EMERY_POWDER.get())) return EMERY;
            if (st.is(Content.GEM_GRIT.get())) return GRIT;
            if (st.is(DIAMOND_DUST)) return DIAMOND;
            return NONE;
        }
    }

    private Cut cut = Cut.STEP;
    private Abrasive loaded = Abrasive.NONE;
    private int charge;
    private int lastQuality;
    @Nullable
    private MachineRecipe recipe;

    public CuttingWheelBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 20000, 200, 0);
    }

    public Cut cut() {
        return cut;
    }

    public void setCut(Cut c) {
        if (c != cut) progress = 0;
        cut = c;
        setChanged();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_IN) return level == null || findRecipe(stack) != null;
        if (slot == SLOT_GRIT) return Abrasive.of(stack) != Abrasive.NONE;
        return super.isItemValid(slot, stack);
    }

    @Nullable
    private MachineRecipe findRecipe(ItemStack in) {
        if (in.isEmpty() || level == null) return null;
        for (MachineRecipe r : Content.GEM_CUTTING.all(level)) {
            if (!r.inputs.isEmpty() && r.inputs.get(0).ingredient.test(in) && in.getCount() >= Math.max(1, r.inputs.get(0).count) && !r.outputs.isEmpty()) {
                return r;
            }
        }
        return null;
    }

    /** The abrasive the next cut would use (loaded charge, else what is in the slot). */
    private Abrasive nextAbrasive() {
        return charge > 0 ? loaded : Abrasive.of(items.getStackInSlot(SLOT_GRIT));
    }

    public int expectedQuality() {
        return cut.base + nextAbrasive().bonus + Math.round(outputBonus() * 100);
    }

    public float shatterChance() {
        return Math.max(0f, cut.shatter - nextAbrasive().steady);
    }

    private boolean hasFreeOutput() {
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role == MachineLayout.Role.OUTPUT && items.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        if (inventoryDirty) {
            inventoryDirty = false;
            MachineRecipe r = findRecipe(items.getStackInSlot(SLOT_IN));
            if (r == null || recipe == null || !r.getId().equals(recipe.getId())) progress = 0;
            recipe = r;
        }
        boolean active = false;
        if (recipe != null && canRun() && hasFreeOutput() && nextAbrasive() != Abrasive.NONE) {
            maxProgress = Math.max(1, (int) Math.round(recipe.time * cut.time * speedMultiplier()));
            int ept = Math.max(1, (int) Math.round(recipe.energy * energyMultiplier()));
            if (energy.getEnergyStored() >= ept) {
                energy.consume(ept);
                rate = ept;
                active = true;
                if (++progress >= maxProgress) {
                    finishCut();
                    progress = 0;
                    inventoryDirty = true;
                }
                setChanged();
            }
        }
        if (!active) rate = 0;
        setLit(active);
    }

    private void finishCut() {
        if (charge <= 0) {
            Abrasive a = Abrasive.of(items.getStackInSlot(SLOT_GRIT));
            if (a == Abrasive.NONE) return;
            items.extractItem(SLOT_GRIT, 1, false);
            loaded = a;
            charge = a.uses;
        }
        ItemStack result = recipe.outputs.get(0).stack.copy();
        int base = cut.base + loaded.bonus + Math.round(outputBonus() * 100);
        float tri = level.random.nextFloat() + level.random.nextFloat() - 1f;
        int quality = Math.max(1, Math.min(100, Math.round(base + tri * cut.spread)));
        float shatter = Math.max(0f, cut.shatter - loaded.steady);
        charge--;
        items.extractItem(SLOT_IN, Math.max(1, recipe.inputs.get(0).count), false);
        if (level.random.nextFloat() < shatter) {
            lastQuality = -1;
            insertOutput(new ItemStack(Content.GEM_GRIT.get(), 2), false);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 0.7f);
            return;
        }
        lastQuality = quality;
        insertOutput(GemData.setCut(result, cut, quality), false);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 0.8f + quality / 100f);
    }

    @Override
    public void onCustomButton(int id, Player player) {
        if (id >= 0 && id < Cut.values().length) setCut(Cut.byIndex(id));
    }

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case 0 -> cut.ordinal();
            case 1 -> lastQuality;
            case 2 -> charge;
            default -> expectedQuality() * 1000 + Math.round(shatterChance() * 100);
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {}

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putString("Cut", cut.id());
        tag.putString("Abrasive", loaded.name());
        tag.putInt("Charge", charge);
        tag.putInt("LastQuality", lastQuality);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        cut = Cut.byId(tag.getString("Cut"));
        try {
            loaded = Abrasive.valueOf(tag.getString("Abrasive"));
        } catch (IllegalArgumentException e) {
            loaded = Abrasive.NONE;
        }
        charge = tag.getInt("Charge");
        lastQuality = tag.getInt("LastQuality");
    }
}
