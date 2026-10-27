package com.frontierworks.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Map;
import java.util.function.Supplier;

/** Burns liquid fuel from its tank: each fuel has an FE/t rate and mB burned per 20 ticks. */
public class FluidGeneratorBE extends GeneratorBE {
    public static final MachineLayout LAYOUT = MachineLayout.generator(false, 1, 16000);

    public static class Fuel {
        public final Supplier<? extends Fluid> fluid;
        public final int fePerTick;
        public final int mbPerSecond;

        public Fuel(Supplier<? extends Fluid> fluid, int fePerTick, int mbPerSecond) {
            this.fluid = fluid;
            this.fePerTick = fePerTick;
            this.mbPerSecond = mbPerSecond;
        }
    }

    private final Map<String, Fuel> fuels;
    private int tickCounter;

    public FluidGeneratorBE(BlockEntityType<?> type, BlockPos pos, BlockState state, Map<String, Fuel> fuels, int capacity) {
        super(type, pos, state, LAYOUT, capacity, 400);
        this.fuels = fuels;
    }

    private Fuel fuelFor(FluidStack fs) {
        if (fs.isEmpty()) return null;
        for (Fuel f : fuels.values()) {
            if (fs.getFluid().isSame(f.fluid.get())) return f;
        }
        return null;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return fuelFor(stack) != null;
    }

    @Override
    protected int generate() {
        FluidTank tank = tanks[0];
        Fuel f = fuelFor(tank.getFluid());
        if (f == null) return 0;
        if (tickCounter++ % 20 == 0) {
            if (tank.getFluidAmount() < f.mbPerSecond) return 0;
            tank.drain(f.mbPerSecond, IFluidHandler.FluidAction.EXECUTE);
        }
        return f.fePerTick;
    }
}
