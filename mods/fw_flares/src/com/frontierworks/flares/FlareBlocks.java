package com.frontierworks.flares;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FlareBlocks {
    private FlareBlocks() {}

    /** Invisible light left by a flare; fades after ~2 minutes. */
    public static class FlareLight extends Block {
        public FlareLight(Properties props) {
            super(props);
        }

        @Override
        @SuppressWarnings("deprecation")
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return Shapes.empty();
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            level.scheduleTick(pos, this, 2400);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            level.removeBlock(pos, false);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.01, 0);
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
            return true;
        }
    }

    /** Small glowing stick that sits on any solid surface. */
    public static class Glowstick extends Block {
        private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 10, 10);

        public Glowstick(Properties props) {
            super(props);
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return Shapes.empty();
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        @SuppressWarnings("deprecation")
        public BlockState updateShape(BlockState state, Direction dir, BlockState n, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos np) {
            return dir == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
        }
    }

    /** Flare item: right-click to throw. */
    public static class FlareItem extends Item {
        public FlareItem(Properties props) {
            super(props);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.5f);
            if (!level.isClientSide) {
                FlareEntity f = new FlareEntity(Content.FLARE_ENTITY.get(), player, level);
                f.setItem(stack);
                f.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.3f, 1.0f);
                level.addFreshEntity(f);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}
