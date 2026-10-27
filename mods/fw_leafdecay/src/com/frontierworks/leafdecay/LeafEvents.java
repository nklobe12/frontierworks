package com.frontierworks.leafdecay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** After a log is removed, nearby unsupported leaves decay within seconds instead of minutes. */
public final class LeafEvents {
    private static final ArrayDeque<Object[]> QUEUE = new ArrayDeque<>(); // {level, pos, dueTick}
    private static final Set<Long> QUEUED = new HashSet<>();

    private LeafEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(LeafEvents::onBreak);
        MinecraftForge.EVENT_BUS.addListener(LeafEvents::onTick);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !event.getState().is(BlockTags.LOGS)) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        long now = level.getGameTime();
        int i = 0;
        for (BlockPos p : BlockPos.betweenClosed(event.getPos().offset(-6, -3, -6), event.getPos().offset(6, 8, 6))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && QUEUED.add(p.asLong())) {
                QUEUE.add(new Object[]{level, p.immutable(), now + 20 + (i++ % 60)});
            }
        }
    }

    private static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) return;
        int budget = 32;
        Iterator<Object[]> it = QUEUE.iterator();
        while (it.hasNext() && budget > 0) {
            Object[] e = it.next();
            ServerLevel level = (ServerLevel) e[0];
            long due = (Long) e[2];
            if (level.getGameTime() < due) continue;
            it.remove();
            BlockPos p = (BlockPos) e[1];
            QUEUED.remove(p.asLong());
            budget--;
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.DISTANCE) >= 7) {
                level.destroyBlock(p, true);
            } else if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && level.random.nextInt(3) == 0 && QUEUED.add(p.asLong())) {
                // distances may still be updating; check once more a bit later
                QUEUE.add(new Object[]{level, p, level.getGameTime() + 40});
            }
        }
    }
}
