package com.lodestar.core.block;

import com.lodestar.core.util.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Dimension gate: right-click to travel between a home dimension and a target dimension at the same
 * X/Z. A matching gate is built at the arrival point if none is nearby, so you can always return.
 */
public class GateBlock extends Block {
    private final String target;
    private final String home;

    public GateBlock(Properties props, String target, String home) {
        super(props);
        this.target = target;
        this.home = home;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer)) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;
        boolean inTarget = level.dimension().location().toString().equals(target);
        ServerLevel dest = TeleportUtil.level(sp.server, inTarget ? home : target);
        if (dest == null) {
            player.displayClientMessage(Component.translatable("msg.ls_core.destination_unavailable"), true);
            return InteractionResult.FAIL;
        }
        int x = pos.getX(), z = pos.getZ();
        BlockPos found = null;
        dest.getChunk(x >> 4, z >> 4);
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(x - 8, dest.getMinBuildHeight(), z - 8), new BlockPos(x + 8, dest.getMinBuildHeight() + dest.getHeight() - 1, z + 8))) {
            if (dest.getBlockState(p).is(this)) {
                found = p.immutable();
                break;
            }
        }
        BlockPos land;
        if (found != null) {
            land = found.north();
            if (!dest.isEmptyBlock(land)) land = found.above();
        } else {
            land = TeleportUtil.findSafe(dest, x, z, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
            BlockPos gate = land.east();
            dest.setBlockAndUpdate(gate, defaultBlockState());
            if (dest.isEmptyBlock(gate.below())) dest.setBlockAndUpdate(gate.below(), net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        }
        level.playSound(null, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.3f, 1.4f);
        TeleportUtil.teleport(sp, dest, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.5, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.5);
        }
    }
}
