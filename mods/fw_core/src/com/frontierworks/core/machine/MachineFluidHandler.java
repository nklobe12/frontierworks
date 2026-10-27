package com.frontierworks.core.machine;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;

/** Exposes machine tanks: fill only input tanks, drain only output tanks. */
public class MachineFluidHandler implements IFluidHandler {
    private final MachineBlockEntity be;

    public MachineFluidHandler(MachineBlockEntity be) {
        this.be = be;
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
        if (resource.isEmpty()) return 0;
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
        if (resource.isEmpty()) return FluidStack.EMPTY;
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
        FluidTank[] tanks = be.getTanks();
        for (int i = 0; i < tanks.length; i++) {
            if (be.getLayout().tanks.get(i).output && !tanks[i].isEmpty()) {
                return tanks[i].drain(maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }
}
