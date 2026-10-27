package com.frontierworks.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Small block classes shared by generated content (wood sets, saplings). */
public final class FWBlocks {
    private FWBlocks() {}

    public static class FlammableBlock extends Block {
        private final int flammability, spread;

        public FlammableBlock(Properties props, int flammability, int spread) {
            super(props);
            this.flammability = flammability;
            this.spread = spread;
        }

        @Override
        public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return true;
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return flammability;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return spread;
        }
    }

    public static class LogBlock extends RotatedPillarBlock {
        @Nullable
        private final Supplier<? extends Block> stripped;

        public LogBlock(Properties props, @Nullable Supplier<? extends Block> stripped) {
            super(props);
            this.stripped = stripped;
        }

        @Override
        public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return true;
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return 5;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return 5;
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action, boolean simulate) {
            if (stripped != null && action == ToolActions.AXE_STRIP) {
                return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
            return super.getToolModifiedState(state, context, action, simulate);
        }
    }

    public static class Leaves extends LeavesBlock {
        public Leaves(Properties props) {
            super(props);
        }

        @Override
        public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return true;
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return 60;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return 30;
        }
    }

    /** Tree grower that uses a configured feature defined in a datapack JSON. */
    public static class DataTreeGrower extends AbstractTreeGrower {
        private final ResourceKey<ConfiguredFeature<?, ?>> key;

        public DataTreeGrower(ResourceLocation feature) {
            this.key = ResourceKey.create(Registries.CONFIGURED_FEATURE, feature);
        }

        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return key;
        }
    }

    public static class Sapling extends SaplingBlock {
        public Sapling(ResourceLocation feature, Properties props) {
            super(new DataTreeGrower(feature), props);
        }
    }
}
