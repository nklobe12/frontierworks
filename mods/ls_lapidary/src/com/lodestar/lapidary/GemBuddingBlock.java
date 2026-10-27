package com.lodestar.lapidary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Budding gem rock found in geodes. On random ticks it sprouts a bud of its gem on a free face, or grows a bud
 * already there by one stage. Can be carried away with Silk Touch.
 */
public class GemBuddingBlock extends Block {
    private final Supplier<? extends Block> cluster;

    public GemBuddingBlock(Properties props, Supplier<? extends Block> cluster) {
        super(props);
        this.cluster = cluster;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) grow(level, pos, Direction.getRandom(random));
    }

    /** One growth step on the given face; returns true if something sprouted or grew. */
    public boolean grow(ServerLevel level, BlockPos pos, Direction dir) {
        BlockPos t = pos.relative(dir);
        BlockState there = level.getBlockState(t);
        Block c = cluster.get();
        if (there.is(c)) {
            if (there.getValue(GemClusterBlock.FACING) != dir || there.getValue(GemClusterBlock.STAGE) >= 3) return false;
            level.setBlock(t, there.setValue(GemClusterBlock.STAGE, there.getValue(GemClusterBlock.STAGE) + 1), 3);
            if (there.getValue(GemClusterBlock.STAGE) == 2) {
                level.playSound(null, t, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.5f, 1.2f);
            }
            return true;
        }
        boolean water = there.is(Blocks.WATER) && there.getFluidState().getAmount() == 8;
        if (there.isAir() || water) {
            level.setBlock(t, c.defaultBlockState().setValue(GemClusterBlock.STAGE, 0).setValue(GemClusterBlock.FACING, dir)
                    .setValue(GemClusterBlock.WATERLOGGED, water), 3);
            return true;
        }
        return false;
    }
}
