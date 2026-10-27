package com.lodestar.dynamos;

import com.lodestar.core.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.function.ToIntFunction;

/** Dynamo that burns fluid 100 mB at a time: magmatic (lava and hot fluids), compression (liquid fuels). */
public class FluidDynamoBE extends DynamoBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(156, 17, 52).tank(80, 17, 16, 52, 8000, true, false).slot(130, 53, MachineLayout.Role.CHARGE)
            .slot(8, 17, MachineLayout.Role.UPGRADE).slot(8, 35, MachineLayout.Role.UPGRADE).slot(8, 53, MachineLayout.Role.UPGRADE)
            .info(30, 20).build();

    /** FE per mB, 0 when the fluid is not a fuel */
    private final ToIntFunction<FluidStack> perMb;

    public FluidDynamoBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int rate, ToIntFunction<FluidStack> perMb) {
        super(type, pos, state, LAYOUT, rate);
        this.perMb = perMb;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return perMb.applyAsInt(stack) > 0;
    }

    @Override
    protected long consumeFuel() {
        FluidStack fs = tanks[0].getFluid();
        int v = perMb.applyAsInt(fs);
        if (v <= 0 || fs.getAmount() < 100) return 0;
        tanks[0].drain(100, IFluidHandler.FluidAction.EXECUTE);
        return (long) v * 100;
    }
}
