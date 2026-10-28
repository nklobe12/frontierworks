package com.lodestar.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A hand wheel mounted on a block face, driving the shaft behind it. Hold use on it to turn it clockwise (sneak to
 * turn it the other way): 24 RPM and 64 Nm while you keep turning, at the cost of a little hunger.
 */
public class HandWheelBE extends KineticBE implements IKineticSource {
    public static final float RPM = 24, TORQUE = 64;
    private int turning;
    private int dir = 1;

    public HandWheelBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public double faceFactor(Direction d) {
        return d == facing().getOpposite() ? 1 : 0;
    }

    @Override
    public float sourceSpeed() {
        if (turning <= 0) return 0;
        // clockwise for someone facing the wheel = spin vector pointing into the mount
        return RPM * dir * -facing().getAxisDirection().getStep();
    }

    @Override
    public float sourceTorque() {
        return TORQUE;
    }

    /** Turns the wheel for a moment (also used by the self-test). */
    public void turn(boolean reverse, int ticks) {
        turning = Math.max(turning, ticks);
        dir = reverse ? -1 : 1;
    }

    @Override
    protected void nodeTick() {
        if (turning > 0) turning--;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            turn(player.isShiftKeyDown(), 8);
            player.causeFoodExhaustion(0.06f);
            if (status != OK) player.displayClientMessage(describe(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
