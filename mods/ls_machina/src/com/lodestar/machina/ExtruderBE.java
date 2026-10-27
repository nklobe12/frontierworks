package com.lodestar.machina;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Igneous Extruder: with lava and water in its tanks it makes rock the way flowing lava does, without spending
 * the fluids for ordinary stones. Obsidian uses up lava. Pick the product with the button.
 */
public class ExtruderBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 52).tank(30, 17, 16, 52, 4000, true, false).tank(50, 17, 16, 52, 4000, true, false)
            .arrow(76, 34).slot(108, 35, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .button(0, 70, 56, 76, 14, "").extra(1).build();

    /** product, lava used, water used, ticks */
    private static final Object[][] PRODUCTS = {
            {Items.COBBLESTONE, 0, 0, 16}, {Items.STONE, 0, 0, 24}, {Items.GRANITE, 0, 0, 32}, {Items.DIORITE, 0, 0, 32},
            {Items.ANDESITE, 0, 0, 32}, {Items.TUFF, 0, 0, 40}, {Items.COBBLED_DEEPSLATE, 50, 0, 40}, {Items.BASALT, 100, 0, 40},
            {Items.BLACKSTONE, 100, 0, 48}, {Items.OBSIDIAN, 1000, 0, 80}};

    private int product;

    public ExtruderBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 20000, 400, 0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 ? stack.getFluid().isSame(Fluids.LAVA) : stack.getFluid().isSame(Fluids.WATER);
    }

    @Override
    public void onCustomButton(int id, Player player) {
        product = (product + 1) % PRODUCTS.length;
        progress = 0;
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        Item it = (Item) PRODUCTS[Math.max(0, Math.min(PRODUCTS.length - 1, menu.extra(0)))][0];
        return new ItemStack(it).getHoverName();
    }


    @Override
    public int getExtra(int i) {
        return product;
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        Object[] p = PRODUCTS[product];
        int lava = (Integer) p[1], water = (Integer) p[2];
        maxProgress = (int) Math.max(2, Math.round((Integer) p[3] * speedMultiplier()));
        int cost = (int) Math.round(20 * energyMultiplier());
        boolean ok = canRun() && tanks[0].getFluidAmount() >= Math.max(1000, lava) && tanks[1].getFluidAmount() >= Math.max(1000, water)
                && energy.getEnergyStored() >= cost && insertOutput(new ItemStack((Item) p[0]), true).isEmpty();
        if (ok) {
            energy.consume(cost);
            rate = cost;
            if (++progress >= maxProgress) {
                progress = 0;
                if (lava > 0) tanks[0].drain(lava, IFluidHandler.FluidAction.EXECUTE);
                if (water > 0) tanks[1].drain(water, IFluidHandler.FluidAction.EXECUTE);
                insertOutput(new ItemStack((Item) p[0]), false);
            }
        } else {
            rate = 0;
        }
        setLit(ok);
        setChanged();
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {}

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Product", product);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        product = Math.max(0, Math.min(PRODUCTS.length - 1, tag.getInt("Product")));
    }
}
