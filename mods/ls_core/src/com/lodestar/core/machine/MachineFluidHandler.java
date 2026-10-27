package com.lodestar.core.machine;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import net.minecraft.core.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Exposes machine tanks: fill only input tanks, drain only output tanks, respecting the side configuration. */
public class MachineFluidHandler implements IFluidHandler {
    private final MachineBlockEntity be;
    @Nullable
    private final Direction side;

    public MachineFluidHandler(MachineBlockEntity be, @Nullable Direction side) {
        this.be = be;
        this.side = side;
    }

    @Override
    public int getTanks() {
        return be.getTanks().length;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return be.getTanks()[tank].getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return be.getTanks()[tank].getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return be.getLayout().tanks.get(tank).input && be.getTanks()[tank].isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !be.sideAllows(side, true)) return 0;
        FluidTank[] tanks = be.getTanks();
        // prefer a tank already holding this fluid
        for (int i = 0; i < tanks.length; i++) {
            if (be.getLayout().tanks.get(i).input && !tanks[i].isEmpty() && tanks[i].getFluid().isFluidEqual(resource)) {
                return tanks[i].fill(resource, action);
            }
        }
        for (int i = 0; i < tanks.length; i++) {
            if (be.getLayout().tanks.get(i).input && tanks[i].isEmpty() && tanks[i].isFluidValid(resource)) {
                return tanks[i].fill(resource, action);
            }
        }
        return 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !be.sideAllows(side, false)) return FluidStack.EMPTY;
        FluidTank[] tanks = be.getTanks();
        for (int i = 0; i < tanks.length; i++) {
            if (be.getLayout().tanks.get(i).output && tanks[i].getFluid().isFluidEqual(resource)) {
                return tanks[i].drain(resource, action);
            }
        }
        return FluidStack.EMPTY;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!be.sideAllows(side, false)) return FluidStack.EMPTY;
        FluidTank[] tanks = be.getTanks();
        for (int i = 0; i < tanks.length; i++) {
            if (be.getLayout().tanks.get(i).output && !tanks[i].isEmpty()) {
                return tanks[i].drain(maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }
}
