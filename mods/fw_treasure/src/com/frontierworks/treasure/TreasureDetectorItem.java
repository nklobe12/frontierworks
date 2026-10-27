package com.frontierworks.treasure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Treasure detector: points to the nearest unopened loot container (chest, barrel...) within 48 blocks. */
public class TreasureDetectorItem extends Item {
    private static final int RANGE = 48;

    public TreasureDetectorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        BlockPos me = player.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        ChunkPos center = new ChunkPos(me);
        int r = (RANGE >> 4) + 1;
        for (int cx = center.x - r; cx <= center.x + r; cx++) {
            for (int cz = center.z - r; cz <= center.z + r; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof RandomizableContainerBlockEntity)) continue;
                    double d = be.getBlockPos().distSqr(me);
                    if (d > RANGE * RANGE || d >= bestD) continue;
                    CompoundTag tag = be.saveWithoutMetadata();
                    if (tag.contains("LootTable")) {
                        best = be.getBlockPos();
                        bestD = d;
                    }
                }
            }
        }
        player.getCooldowns().addCooldown(this, 40);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        if (best == null) {
            player.displayClientMessage(Component.translatable("msg.fw_treasure.detector_none").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, me, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
        } else {
            int dist = (int) Math.sqrt(bestD);
            int dy = best.getY() - me.getY();
            double ang = Math.toDegrees(Math.atan2(best.getZ() - me.getZ(), best.getX() - me.getX())) - 90.0 - player.getYRot();
            ang = ((ang % 360) + 540) % 360 - 180;
            String dir = Math.abs(ang) < 30 ? "ahead" : Math.abs(ang) > 150 ? "behind" : ang > 0 ? "right" : "left";
            String vert = dy > 3 ? "above" : dy < -3 ? "below" : "level";
            player.displayClientMessage(Component.translatable("msg.fw_treasure.detector",
                    Component.translatable("msg.fw_treasure.dir." + dir), dist, Component.translatable("msg.fw_treasure.vert." + vert))
                    .withStyle(ChatFormatting.GOLD), true);
            float pitch = 2.0f - Math.min(1.5f, dist / 32.0f);
            level.playSound(null, me, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, pitch);
        }
        return InteractionResultHolder.success(stack);
    }
}
