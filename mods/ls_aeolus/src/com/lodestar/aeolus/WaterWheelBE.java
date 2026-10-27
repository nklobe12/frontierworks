package com.lodestar.aeolus;

import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Water wheel. The eight blocks around its hub (in the plane across its axle) must be air or water. Each block of moving water
 * there pushes the wheel along its rim: falling water pushes down, flowing water along its current. The pushes add up into a
 * torque (opposite pushes cancel); still water drags. Power leaves through the two ends of the axle.
 */
public class WaterWheelBE extends MachineBlockEntity {
    public static final int OK = 0, STALLED = 1, DRY = 2;
    public static final MachineLayout LAYOUT = MachineLayout.builder().slot(130, 53, MachineLayout.Role.CHARGE).energy(156, 17, 52)
            .info(8, 20).extra(5).build();
    public static final double FE_PER_TORQUE = 5.0;
    public static final int MAX_RATE = 40;
    private static final int CAPACITY = 20_000, PUSH = 200;

    public int status = DRY;
    public double torque;
    public int moving, still, blocked;
    private int scanTimer = 100;

    public float targetSpeed, speed, angle, prevAngle;
    private float syncedSpeed = Float.NaN;

    public WaterWheelBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, CAPACITY, 0, PUSH);
    }

    @Override
    public boolean hasSideConfig() {
        return false;
    }

    @Override
    public boolean acceptsAugment(AugmentItem a) {
        return false;
    }

    /** Recomputes torque from the water around the hub. */
    public void scan() {
        Direction across = facing().getClockWise();
        torque = 0;
        moving = still = blocked = 0;
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = worldPosition.relative(across, a).relative(Direction.UP, b);
                BlockState st = level.getBlockState(p);
                FluidState fs = st.getFluidState();
                if (!fs.isEmpty() && fs.is(FluidTags.WATER)) {
                    if (fs.isSource()) {
                        still++;
                        continue;
                    }
                    Vec3 flow = fs.getFlow(level, p);
                    double fu = flow.x * across.getStepX() + flow.z * across.getStepZ();
                    double fv = fs.hasProperty(FlowingFluid.FALLING) && fs.getValue(FlowingFluid.FALLING) ? -1 : flow.y;
                    // push along the rim: tangent of a counter-clockwise turn at offset (a, b) is (-b, a)
                    torque += (fu * -b + fv * a) / Math.sqrt(a * a + b * b);
                    moving++;
                } else if (!st.isAir() && !st.getCollisionShape(level, p).isEmpty()) {
                    blocked++;
                }
            }
        status = blocked > 0 ? STALLED : moving == 0 ? DRY : OK;
    }

    /** Energy per tick for the current water. */
    public int output() {
        if (status != OK) return 0;
        double drag = Math.max(0.4, 1 - 0.08 * still);
        return (int) Math.min(MAX_RATE, Math.round(Math.abs(torque) * FE_PER_TORQUE * drag));
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        if (++scanTimer >= 10) {
            scanTimer = 0;
            scan();
        }
        int made = 0;
        if (canRun() && !energy.isFull()) {
            made = output();
            energy.produce(made);
        }
        rate = made;
        chargeItems(PUSH);
        Direction axle = facing();
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, PUSH, d -> d.getAxis() == axle.getAxis());
        setLit(made > 0);
        float want = status == OK ? (float) (Math.signum(torque) * Math.min(7, 0.8 + Math.abs(torque) * 1.4)) : 0f;
        if (Float.isNaN(syncedSpeed) || Math.abs(want - syncedSpeed) > 0.3f) {
            syncedSpeed = want;
            targetSpeed = want;
            sync();
        }
        setChanged();
    }

    @Override
    public void clientTick() {
        speed += (targetSpeed - speed) * 0.05f;
        if (Math.abs(angle) > 3600f) angle -= Math.signum(angle) * 3600f;
        prevAngle = angle;
        angle += speed;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null && side.getAxis() != facing().getAxis()) return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case 0 -> rate;
            case 1 -> status;
            case 2 -> moving;
            case 3 -> (int) Math.round(torque * 100);
            default -> blocked;
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_aeolus.output", menu.extra(0)));
        lines.add(Component.translatable("gui.ls_aeolus.flow", menu.extra(2), String.format("%.2f", Math.abs(menu.extra(3)) / 100.0)));
        int st = menu.extra(1);
        if (st == STALLED) lines.add(Component.translatable("gui.ls_aeolus.status.stalled", menu.extra(4)).withStyle(ChatFormatting.DARK_RED));
        else if (st == DRY) lines.add(Component.translatable("gui.ls_aeolus.status.dry").withStyle(ChatFormatting.DARK_RED));
        else lines.add(Component.translatable("gui.ls_aeolus.status.ok").withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    protected void saveSyncExtra(CompoundTag tag) {
        tag.putFloat("Speed", targetSpeed);
    }

    @Override
    protected void loadSyncExtra(CompoundTag tag) {
        targetSpeed = tag.getFloat("Speed");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.6);
    }
}
