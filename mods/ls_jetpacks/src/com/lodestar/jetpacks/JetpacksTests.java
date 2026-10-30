package com.lodestar.jetpacks;

import com.lodestar.core.energy.ItemEnergy;
import com.lodestar.core.test.SelfTest;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

/** Self-test: thrust lifts a player and burns fuel/energy; only fuels fill a fuel jetpack; upgrades install by crafting. */
final class JetpacksTests {
    private static final Logger LOG = LogUtils.getLogger();

    private JetpacksTests() {}

    static void register() {
        SelfTest.scenario("jetpacks:flight", 5, JetpacksTests::flight);
    }

    private static Supplier<String> flight(ServerLevel l, BlockPos o) {
        return () -> {
            FakePlayer fp = FakePlayerFactory.getMinecraft(l);
            fp.setPos(o.getX() + 0.5, o.getY() + 20, o.getZ() + 0.5);
            ItemStack before = fp.getItemBySlot(EquipmentSlot.CHEST).copy();
            try {
                // electric: thrust for 10 ticks
                ItemStack pack = JetpacksSetup.full(JetpackTier.ELECTRUM, Fluids.EMPTY);
                fp.setItemSlot(EquipmentSlot.CHEST, pack);
                fp.setDeltaMovement(0, -0.5, 0);
                int e0 = ItemEnergy.get(pack);
                for (int i = 0; i < 10; i++) {
                    JetpackLogic.move(fp, fp.getItemBySlot(EquipmentSlot.CHEST), JetpackLogic.JUMP | JetpackLogic.FWD);
                    JetpackLogic.setInput(fp, JetpackLogic.JUMP);
                    JetpackLogic.serverTick(fp);
                }
                double vy = fp.getDeltaMovement().y;
                int e1 = ItemEnergy.get(fp.getItemBySlot(EquipmentSlot.CHEST));
                // fuel: refuses water, takes creosote, burns it
                Fluid creosote = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("ls_machina", "creosote"));
                ItemStack fuelPack = new ItemStack(JetpacksSetup.jetpack(JetpackTier.BRONZE));
                int water = FluidUtil.getFluidHandler(fuelPack).map(h -> h.fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)).orElse(-1);
                int crea = FluidUtil.getFluidHandler(fuelPack).map(h -> h.fill(new FluidStack(creosote, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)).orElse(-1);
                fp.setItemSlot(EquipmentSlot.CHEST, fuelPack);
                for (int i = 0; i < 40; i++) {
                    JetpackLogic.setInput(fp, JetpackLogic.JUMP);
                    JetpackLogic.serverTick(fp);
                }
                int left = ((JetpackItem) fuelPack.getItem()).fuel(fp.getItemBySlot(EquipmentSlot.CHEST)).getAmount();
                // upgrade recipe
                ItemStack up = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_jetpacks", "thrust_booster")));
                var inv = new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) {
                        return ItemStack.EMPTY;
                    }

                    public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                        return true;
                    }
                }, 3, 3);
                inv.setItem(0, new ItemStack(JetpacksSetup.jetpack(JetpackTier.STEEL)));
                inv.setItem(4, up);
                var recipe = new JetpackUpgradeRecipe(new ResourceLocation("ls_jetpacks", "test"), net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
                boolean matches = recipe.matches(inv, l);
                int lvl = matches ? JetpackItem.level(recipe.assemble(inv, l.registryAccess()), JetpackItem.Mod.THRUST) : -1;
                LOG.info("jetpacks:flight electrum vy={} energy {}->{}; bronze water={} creosote={} left={}; upgrade level={}", vy, e0, e1, water, crea, left, lvl);
                JetpackLogic.forget(fp.getUUID());
                if (vy <= 0) return "thrust did not lift the player (vy=" + vy + ")";
                if (e1 >= e0) return "thrust used no energy";
                if (water != 0) return "a fuel jetpack accepted water";
                if (crea != 1000) return "a fuel jetpack took " + crea + " mB of creosote";
                if (left >= 1000) return "thrust burnt no fuel";
                if (lvl != 1) return "upgrade recipe gave level " + lvl;
                return null;
            } finally {
                fp.setItemSlot(EquipmentSlot.CHEST, before);
            }
        };
    }
}
