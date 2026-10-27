package com.frontierworks.elevators;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Jump on an elevator block to go up to the next one, sneak to go down (within 24 blocks). */
public final class ElevatorEvents {
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();
    private static final int RANGE = 24;

    private ElevatorEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ElevatorEvents::onJump);
        MinecraftForge.EVENT_BUS.addListener(ElevatorEvents::onTick);
    }

    public static class ElevatorBlock extends Block {
        public ElevatorBlock(Properties props) {
            super(props);
        }
    }

    private static boolean isElevator(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ElevatorBlock;
    }

    private static boolean clear(Level level, BlockPos pos) {
        return !level.getBlockState(pos.above()).isSuffocating(level, pos.above()) && !level.getBlockState(pos.above(2)).isSuffocating(level, pos.above(2));
    }

    private static void move(Player p, int dir) {
        Level level = p.level();
        BlockPos base = p.blockPosition().below();
        if (!isElevator(level, base)) return;
        for (int i = 1; i <= RANGE; i++) {
            BlockPos t = base.above(i * dir);
            if (isElevator(level, t) && clear(level, t)) {
                p.teleportTo(p.getX(), t.getY() + 1, p.getZ());
                p.fallDistance = 0;
                level.playSound(null, t, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
                return;
            }
        }
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            move((Player) event.getEntity(), 1);
        }
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer)) return;
        Player p = event.player;
        boolean sneak = p.isShiftKeyDown();
        Boolean was = wasSneaking.put(p.getUUID(), sneak);
        if (sneak && (was == null || !was)) move(p, -1);
    }
}
