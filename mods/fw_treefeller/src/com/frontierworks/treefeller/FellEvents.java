package com.frontierworks.treefeller;

import com.frontierworks.core.util.AreaMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Chop the bottom of a tree with an axe and the whole trunk comes down (up to 192 logs). Only natural
 * trees (logs touching leaves) are felled; sneak to chop a single log.
 */
public final class FellEvents {
    private FellEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(FellEvents::onBreak);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer) || AreaMining.isBusy()) return;
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        if (player.isShiftKeyDown() || player.isCreative()) return;
        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AxeItem)) return;
        Level level = player.level();
        List<BlockPos> logs = AreaMining.connected(level, event.getPos(), s -> s.is(BlockTags.LOGS), 192, true);
        if (logs.size() < 2 || !hasLeaves(level, logs)) return;
        logs.removeIf(p -> p.getY() < event.getPos().getY());
        logs.sort(Comparator.comparingInt(BlockPos::getY));
        AreaMining.breakAll(player, logs, s -> s.is(BlockTags.LOGS), 100f);
    }

    private static boolean hasLeaves(Level level, List<BlockPos> logs) {
        for (BlockPos p : logs) {
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) return true;
            }
        }
        return false;
    }
}
