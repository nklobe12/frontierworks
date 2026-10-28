package com.lodestar.kinetics;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A straight shaft along its axis: axles, the redstone coupling and the two gauges, which also feed comparators. */
public class AxleBE extends KineticBE implements LSEntityBlock.IComparatorOutput {
    public enum Mode { AXLE, COUPLING, TACHOMETER, DYNAMOMETER }

    public final Mode mode;
    private int lastSignal = -1;

    public AxleBE(BlockEntityType<?> type, BlockPos pos, BlockState state, Mode mode) {
        super(type, pos, state);
        this.mode = mode;
    }

    public boolean uncoupled() {
        BlockState s = getBlockState();
        return mode == Mode.COUPLING && s.hasProperty(CouplingBlock.POWERED) && s.getValue(CouplingBlock.POWERED);
    }

    @Override
    public double faceFactor(Direction d) {
        return d.getAxis() == axis() && !uncoupled() ? 1 : 0;
    }

    @Override
    protected boolean syncsLoad() {
        return mode == Mode.DYNAMOMETER;
    }

    /** Needle position 0..1: speed for the tachometer (full scale 256 RPM), load for the dynamometer. */
    public float gaugeValue() {
        return mode == Mode.DYNAMOMETER ? Math.min(1f, load) : Math.min(1f, Math.abs(speed) / KineticGrid.MAX_RPM);
    }

    @Override
    public int comparatorOutput() {
        if (mode == Mode.DYNAMOMETER) return status == OVERLOADED ? 15 : Math.round(14 * Math.min(1f, load));
        if (mode == Mode.TACHOMETER) return Math.min(15, (int) Math.ceil(Math.abs(speed) / 16f));
        return 0;
    }

    @Override
    protected void onSpeedChanged() {
        if (level == null || (mode != Mode.TACHOMETER && mode != Mode.DYNAMOMETER)) return;
        int s = comparatorOutput();
        if (s != lastSignal) {
            lastSignal = s;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public Component describe() {
        if (mode == Mode.DYNAMOMETER) {
            KineticGrid g = currentGrid();
            if (g != null) {
                return Component.translatable("msg.ls_kinetics.dynamometer", Math.round(g.demand), Math.round(g.capacity),
                        g.capacity > 0 ? Math.round(100 * g.demand / g.capacity) : 0, fmt(speed)).withStyle(status == OVERLOADED ? ChatFormatting.RED : ChatFormatting.YELLOW);
            }
        }
        if (mode == Mode.COUPLING && uncoupled()) return Component.translatable("msg.ls_kinetics.uncoupled").withStyle(ChatFormatting.GRAY);
        return super.describe();
    }
}
