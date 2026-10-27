package com.frontierworks.crystals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Supplier;

/** Grows crystal buds on its faces through small, medium and large stages to a full cluster. */
public class BuddingCrystalBlock extends Block {
    private final Supplier<? extends Block>[] stages;

    @SafeVarargs
    public BuddingCrystalBlock(Properties props, Supplier<? extends Block>... stages) {
        super(props);
        this.stages = stages;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(dir);
        BlockState there = level.getBlockState(target);
        Block next = null;
        if (there.isAir() || (there.is(Blocks.WATER) && there.getFluidState().getAmount() == 8)) {
            next = stages[0].get();
        } else {
            for (int i = 0; i < stages.length - 1; i++) {
                if (there.is(stages[i].get()) && there.getValue(AmethystClusterBlock.FACING) == dir) {
                    next = stages[i + 1].get();
                    break;
                }
            }
        }
        if (next != null) {
            BlockState grown = next.defaultBlockState().setValue(AmethystClusterBlock.FACING, dir)
                    .setValue(AmethystClusterBlock.WATERLOGGED, there.getFluidState().getType() == Fluids.WATER);
            level.setBlockAndUpdate(target, grown);
        }
    }
}
