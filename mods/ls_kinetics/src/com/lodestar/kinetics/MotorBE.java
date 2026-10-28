package com.lodestar.kinetics;

import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.energy.LSEnergyStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Kinetic motor: turns Forge Energy into rotation on its front shaft. Use it to pick a speed (16 to 256 RPM), sneak-use
 * it to reverse. It delivers up to 512 Nm and draws power for the torque actually used:
 * FE/t = RPM x Nm used / 256 x 1.25, at least 1 FE/t while turning. A redstone signal stops it.
 */
public class MotorBE extends KineticBE implements IKineticSource {
    public static final int[] SPEEDS = {16, 32, 64, 128, 256};
    public static final float TORQUE = 512;
    private final LSEnergyStorage energy = new LSEnergyStorage(40000, 4000, 0, this::setChanged);
    private final LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
    private int speedIdx = 1;
    private boolean reversed;
    private boolean starved;
    private int lastDraw;

    public MotorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public double faceFactor(Direction d) {
        return d == facing() ? 1 : 0;
    }

    public LSEnergyStorage energy() {
        return energy;
    }

    public int targetSpeed() {
        return SPEEDS[speedIdx];
    }

    public void setTarget(int idx, boolean rev) {
        speedIdx = Math.max(0, Math.min(SPEEDS.length - 1, idx));
        reversed = rev;
        setChanged();
    }

    @Override
    public float sourceSpeed() {
        if (starved || level == null || level.hasNeighborSignal(worldPosition) || energy.getEnergyStored() <= 0) return 0;
        return SPEEDS[speedIdx] * (reversed ? -1 : 1) * facing().getAxisDirection().getStep();
    }

    @Override
    public float sourceTorque() {
        return TORQUE;
    }

    @Override
    public void networkLoad(float utilization, boolean running) {
        if (!running || sourceSpeed() == 0) {
            lastDraw = 0;
            if (starved && energy.getEnergyStored() >= 100) starved = false;
            return;
        }
        int need = Math.max(1, (int) Math.ceil(Math.abs(speed) * TORQUE * utilization / 256f * 1.25f));
        int got = energy.consume(need);
        lastDraw = got;
        if (got < need) starved = true;
    }

    public int lastDraw() {
        return lastDraw;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() || level == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) reversed = !reversed;
            else speedIdx = (speedIdx + 1) % SPEEDS.length;
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, 1.2f);
            player.displayClientMessage(describe(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Component describe() {
        return Component.translatable("msg.ls_kinetics.motor", SPEEDS[speedIdx], Component.translatable(reversed ? "msg.ls_kinetics.ccw" : "msg.ls_kinetics.cw"),
                EnergyUtil.format(energy.getEnergyStored()), lastDraw).withStyle(starved ? ChatFormatting.RED : ChatFormatting.YELLOW);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> c, @Nullable Direction side) {
        if (c == ForgeCapabilities.ENERGY && side != facing()) return cap.cast();
        return super.getCapability(c, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        energy.save(tag, "Energy");
        tag.putInt("SpeedIdx", speedIdx);
        tag.putBoolean("Rev", reversed);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        energy.load(tag, "Energy");
        speedIdx = tag.contains("SpeedIdx") ? Math.max(0, Math.min(SPEEDS.length - 1, tag.getInt("SpeedIdx"))) : 1;
        reversed = tag.getBoolean("Rev");
    }
}
