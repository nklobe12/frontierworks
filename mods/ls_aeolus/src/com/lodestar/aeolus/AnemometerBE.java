package com.lodestar.aeolus;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Anemometer and wind vane: its cups spin with the local wind and its vane points into it (computed on the client from the
 * shared wind model). Use it for a reading; a comparator reads wind strength (0-15).
 */
public class AnemometerBE extends LSBlockEntity implements LSEntityBlock.IInteractable, LSEntityBlock.IComparatorOutput {
    public int signal = -1;
    public float spin, angle, prevAngle, vane, prevVane;
    private boolean vaneSet;

    public AnemometerBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int signalFor(double local) {
        return (int) Math.max(0, Math.min(15, Math.round(local / 2.5 * 15)));
    }

    @Override
    public void tickServer() {
        if (level == null || level.getGameTime() % 20 != Math.floorMod(worldPosition.hashCode(), 20)) return;
        int s = signalFor(Wind.local(level, worldPosition));
        if (s != signal) {
            signal = s;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void tickClient() {
        if (level == null) return;
        double local = Wind.local(level, worldPosition);
        spin += ((float) (local * 16) - spin) * 0.05f;
        if (angle > 3600f) angle -= 3600f;
        prevAngle = angle;
        angle += spin;
        float target = (float) Math.toDegrees(Wind.bearing(level)) + (float) Math.sin(level.getGameTime() * 0.13 + worldPosition.getX()) * 4f * (float) local;
        if (!vaneSet) {
            vane = prevVane = target;
            vaneSet = true;
        }
        prevVane = vane;
        float diff = ((target - vane) % 360f + 540f) % 360f - 180f;
        vane += diff * 0.08f;
    }

    @Override
    public int comparatorOutput() {
        return level == null ? 0 : Math.max(0, signal < 0 ? signalFor(Wind.local(level, worldPosition)) : signal);
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!Wind.hasWind(level)) {
                player.displayClientMessage(Component.translatable("msg.ls_aeolus.no_wind").withStyle(ChatFormatting.GRAY), true);
            } else {
                double local = Wind.local(level, worldPosition);
                String kind = local < 0.4 ? "calm" : local < 0.9 ? "breeze" : local < 1.5 ? "strong" : "gale";
                player.displayClientMessage(Component.translatable("msg.ls_aeolus.reading", String.format("%.1f", Wind.metres(local)),
                        Wind.compassLong(Wind.bearing(level)), Component.translatable("msg.ls_aeolus." + kind),
                        Math.round(local * 100)).withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5).expandTowards(0, 0.6, 0);
    }
}
