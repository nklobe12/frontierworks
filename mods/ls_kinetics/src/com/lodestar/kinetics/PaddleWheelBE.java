package com.lodestar.kinetics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A three-block paddle wheel on a horizontal axle. Moving water in the ring of eight cells around the axle pushes the
 * paddles: water flowing past underneath, or falling down one side. Each wet cell adds speed by how well its current
 * lines up with the paddles' motion, up to 16 RPM, with 256 Nm of torque. The ring must be open: air or water only.
 */
public class PaddleWheelBE extends KineticBE implements IKineticSource {
    public static final float TORQUE = 256, MAX_RPM = 16;
    private float wheelSpeed;
    private boolean obstructed;
    private int wetCells;

    public PaddleWheelBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public double faceFactor(Direction d) {
        return d.getAxis() == axis() ? 1 : 0;
    }

    @Override
    public float sourceSpeed() {
        return wheelSpeed;
    }

    @Override
    public float sourceTorque() {
        return wheelSpeed != 0 ? TORQUE : 0;
    }

    @Override
    protected void nodeTick() {
        if (level != null && (level.getGameTime() + Math.floorMod(worldPosition.hashCode(), 10)) % 10 == 0) sample();
    }

    /** Reads the water around the wheel. */
    public void sample() {
        if (level == null) return;
        Direction.Axis ax = axis();
        Vec3 axisVec = ax == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        double total = 0;
        int wet = 0;
        boolean blocked = false;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                // offsets in the wheel's plane: the horizontal axis across the axle, and up
                Vec3 r = ax == Direction.Axis.X ? new Vec3(0, b, a) : new Vec3(a, b, 0);
                BlockPos p = worldPosition.offset((int) r.x, (int) r.y, (int) r.z);
                BlockState st = level.getBlockState(p);
                FluidState fs = st.getFluidState();
                if (!st.isAir() && !fs.is(FluidTags.WATER) && !st.canBeReplaced()) {
                    blocked = true;
                    continue;
                }
                if (!fs.is(FluidTags.WATER)) continue;
                Vec3 flow = flowOf(p, fs);
                if (flow.lengthSqr() < 1e-4) continue;
                Vec3 t = axisVec.cross(r).normalize();
                double c = flow.normalize().dot(t);
                if (Math.abs(c) > 0.2) wet++;
                total += c;
            }
        }
        obstructed = blocked;
        wetCells = wet;
        float sp = blocked ? 0 : (float) Math.max(-MAX_RPM, Math.min(MAX_RPM, Math.round(total * 4)));
        wheelSpeed = sp;
    }

    private Vec3 flowOf(BlockPos p, FluidState fs) {
        if (fs.isSource()) return Vec3.ZERO;
        if (fs.hasProperty(FlowingFluid.FALLING) && fs.getValue(FlowingFluid.FALLING)) return new Vec3(0, -1, 0);
        return fs.getFlow(level, p);
    }

    public boolean obstructed() {
        return obstructed;
    }

    public int wetCells() {
        return wetCells;
    }

    @Override
    public Component describe() {
        if (obstructed) return Component.translatable("msg.ls_kinetics.paddle_blocked").withStyle(ChatFormatting.RED);
        if (wheelSpeed == 0) return Component.translatable("msg.ls_kinetics.paddle_still").withStyle(ChatFormatting.GRAY);
        return Component.translatable("msg.ls_kinetics.paddle", fmt(Math.abs(wheelSpeed)), wetCells).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("  ")).append(super.describe());
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.1);
    }
}
