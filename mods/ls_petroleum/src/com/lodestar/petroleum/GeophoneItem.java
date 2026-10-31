package com.lodestar.petroleum;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Pressed into the ground, the geophone listens for oil: it reports the reservoir under the chunk (size and buckets
 * left) and the richest reservoir within three chunks, with its compass direction.
 */
public class GeophoneItem extends Item {
    public static final int RANGE = 3;

    public GeophoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        level.playSound(player, pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
        if (level.isClientSide || player == null || !(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        player.getCooldowns().addCooldown(this, 30);
        ctx.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
        sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 6, 0.3, 0.02, 0.3, 0.01);
        if (!OilField.hasOil(level)) {
            player.displayClientMessage(Component.translatable("msg.ls_petroleum.geophone.wrong_dim").withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }
        ChunkPos here = new ChunkPos(pos);
        int left = OilField.remaining(sl, here);
        int initial = OilField.initial(sl, here);
        if (initial <= 0) {
            player.displayClientMessage(Component.translatable("msg.ls_petroleum.geophone.none").withStyle(ChatFormatting.GRAY), false);
        } else if (left <= 0) {
            player.displayClientMessage(Component.translatable("msg.ls_petroleum.geophone.depleted").withStyle(ChatFormatting.GRAY), false);
        } else {
            player.displayClientMessage(Component.translatable("msg.ls_petroleum.geophone.here",
                    Component.translatable("oil.ls_petroleum." + OilField.sizeKey(initial)), String.format("%,d", left / 1000))
                    .withStyle(ChatFormatting.GOLD), false);
        }
        int best = 0, bx = 0, bz = 0;
        for (int dx = -RANGE; dx <= RANGE; dx++)
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                if (dx == 0 && dz == 0) continue;
                int r = OilField.remaining(sl, new ChunkPos(here.x + dx, here.z + dz));
                if (r > best) {
                    best = r;
                    bx = dx;
                    bz = dz;
                }
            }
        if (best > left && best > 0) {
            int dist = Math.max(Math.abs(bx), Math.abs(bz));
            player.displayClientMessage(Component.translatable("msg.ls_petroleum.geophone.best", dist,
                    Component.translatable("dir.ls_petroleum." + direction(bx, bz)),
                    Component.translatable("oil.ls_petroleum." + OilField.sizeKey(best))).withStyle(ChatFormatting.YELLOW), false);
        } else if (best == 0) {
            player.displayClientMessage(Component.translatable("msg.ls_petroleum.geophone.nothing_near", RANGE).withStyle(ChatFormatting.GRAY), false);
        }
        return InteractionResult.CONSUME;
    }

    static String direction(int dx, int dz) {
        double ang = Math.toDegrees(Math.atan2(dx, -dz));
        if (ang < 0) ang += 360;
        String[] names = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        return names[(int) Math.round(ang / 45.0) % 8];
    }
}
