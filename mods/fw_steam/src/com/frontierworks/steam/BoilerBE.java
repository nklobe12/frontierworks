package com.frontierworks.steam;

import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Solid-fuel boiler: burns furnace fuel to boil water into steam. Heats up over time; steam output
 * scales with heat. Pushes steam into adjacent fluid handlers (steam engines, turbines, tanks).
 */
public class BoilerBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .tank(26, 17, 16, 52, 16000, true, false)
            .tank(134, 17, 16, 52, 16000, false, true)
            .slot(80, 53, MachineLayout.Role.FUEL).flame(81, 35)
            .info(52, 18).extra(1).build();
    private final int maxSteam;
    private int heat; // 0..1000

    public BoilerBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxSteam) {
        super(type, pos, state, LAYOUT, 0, 0, 0);
        this.maxSteam = maxSteam;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 ? stack.getFluid().isSame(Fluids.WATER) : stack.getFluid().isSame(Content.STEAM.get());
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean burning = false;
        if (burn <= 0 && tanks[0].getFluidAmount() > 0 && tanks[1].getSpace() > 0) {
            ItemStack st = items.getStackInSlot(0);
            int t = ForgeHooks.getBurnTime(st, RecipeType.SMELTING);
            if (t > 0) {
                ItemStack rem = st.getCraftingRemainingItem();
                items.extractItem(0, 1, false);
                if (!rem.isEmpty() && items.getStackInSlot(0).isEmpty()) items.setStackInSlot(0, rem);
                burn = t;
                maxBurn = t;
            }
        }
        if (burn > 0) {
            burn--;
            burning = true;
            heat = Math.min(1000, heat + 2);
        } else {
            heat = Math.max(0, heat - 1);
        }
        int steam = maxSteam * heat / 1000;
        if (steam > 0 && tanks[0].getFluidAmount() >= steam) {
            int space = tanks[1].getSpace();
            int made = Math.min(steam, space);
            if (made > 0) {
                tanks[0].drain(made, IFluidHandler.FluidAction.EXECUTE);
                tanks[1].fill(new FluidStack(Content.STEAM.get(), made), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        // push steam to neighbours
        if (!tanks[1].isEmpty()) {
            for (Direction d : Direction.values()) {
                BlockEntity be = level.getBlockEntity(worldPosition.relative(d));
                if (be == null || be instanceof BoilerBE) continue;
                IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).orElse(null);
                if (h == null) continue;
                int filled = h.fill(tanks[1].getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) tanks[1].drain(filled, IFluidHandler.FluidAction.EXECUTE);
                if (tanks[1].isEmpty()) break;
            }
        }
        setChanged();
        setLit(burning);
    }

    @Override
    public int getExtra(int i) {
        return heat;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.fw_steam.heat", menu.extra(0) / 10));
        lines.add(Component.translatable("gui.fw_steam.steam_rate", maxSteam * menu.extra(0) / 1000));
    }

    @Override
    protected void saveExtra(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("Heat", heat);
    }

    @Override
    protected void loadExtra(net.minecraft.nbt.CompoundTag tag) {
        heat = tag.getInt("Heat");
    }
}
