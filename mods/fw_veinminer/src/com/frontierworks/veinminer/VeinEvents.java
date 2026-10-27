package com.frontierworks.veinminer;

import com.frontierworks.core.util.AreaMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;

import java.util.List;

/** Sneak while mining an ore to mine the whole connected vein (up to 64 blocks) with your tool. */
public final class VeinEvents {
    public static final int LIMIT = 64;

    private VeinEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(VeinEvents::onBreak);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer) || AreaMining.isBusy()) return;
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        if (!player.isShiftKeyDown() || player.isCreative()) return;
        BlockState state = event.getState();
        if (!state.is(Tags.Blocks.ORES)) return;
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || !tool.isCorrectToolForDrops(state)) return;
        Block block = state.getBlock();
        BlockPos origin = event.getPos();
        List<BlockPos> vein = AreaMining.connected(player.level(), origin, s -> s.getBlock() == block, LIMIT, true);
        vein.remove(origin);
        int broken = AreaMining.breakAll(player, vein, s -> s.getBlock() == block, 100f);
        player.causeFoodExhaustion(0.05f * broken);
    }
}
