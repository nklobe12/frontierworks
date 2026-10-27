package com.frontierworks.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public final class RedstoneBlocks {
    private RedstoneBlocks() {}

    /** Base for pulse-triggered directional blocks. */
    public abstract static class Triggered extends DirectionalBlock {
        public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

        protected Triggered(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, TRIGGERED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
        }

        @Override
        @SuppressWarnings("deprecation")
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean was = state.getValue(TRIGGERED);
            if (powered && !was) {
                level.setBlock(pos, state.setValue(TRIGGERED, true), 4);
                if (level instanceof ServerLevel) fire((ServerLevel) level, pos, state.getValue(FACING));
            } else if (!powered && was) {
                level.setBlock(pos, state.setValue(TRIGGERED, false), 4);
            }
        }

        protected abstract void fire(ServerLevel level, BlockPos pos, Direction facing);

        protected static IItemHandler behind(Level level, BlockPos pos, Direction facing) {
            BlockEntity be = level.getBlockEntity(pos.relative(facing.getOpposite()));
            return be == null ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER, facing).orElse(null);
        }
    }

    /** Breaks the block in front on a redstone pulse; drops go into a container behind it if present. */
    public static class Breaker extends Triggered {
        public Breaker(Properties props) {
            super(props);
        }

        @Override
        protected void fire(ServerLevel level, BlockPos pos, Direction facing) {
            BlockPos target = pos.relative(facing);
            BlockState st = level.getBlockState(target);
            if (st.isAir() || st.getDestroySpeed(level, target) < 0 || !st.getFluidState().isEmpty() && st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return;
            List<ItemStack> drops = Block.getDrops(st, level, target, level.getBlockEntity(target), null, new ItemStack(Items.IRON_PICKAXE));
            level.destroyBlock(target, false);
            IItemHandler h = behind(level, pos, facing);
            for (ItemStack d : drops) {
                ItemStack rem = h == null ? d : ItemHandlerHelper.insertItem(h, d, false);
                if (!rem.isEmpty()) Block.popResource(level, target, rem);
            }
        }
    }

    /** Places a block from the container behind it into the space in front on a redstone pulse. */
    public static class Placer extends Triggered {
        public Placer(Properties props) {
            super(props);
        }

        @Override
        protected void fire(ServerLevel level, BlockPos pos, Direction facing) {
            BlockPos target = pos.relative(facing);
            if (!level.getBlockState(target).canBeReplaced()) return;
            IItemHandler h = behind(level, pos, facing);
            if (h == null) return;
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack st = h.getStackInSlot(i);
                if (st.getItem() instanceof BlockItem) {
                    ItemStack one = h.extractItem(i, 1, true);
                    if (one.isEmpty()) continue;
                    InteractionResult r = ((BlockItem) one.getItem()).place(new DirectionalPlaceContext(level, target, facing, one, facing.getOpposite()));
                    if (r.consumesAction()) {
                        h.extractItem(i, 1, false);
                        return;
                    }
                }
            }
        }
    }

    /** Emits a redstone signal while a player is within 8 blocks. */
    public static class PlayerDetector extends Block {
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        public PlayerDetector(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(POWERED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(POWERED);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            level.scheduleTick(pos, this, 10);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean near = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8), p -> !p.isSpectator()).isEmpty();
            if (near != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, near), 3);
            level.scheduleTick(pos, this, 10);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return state.getValue(POWERED) ? 15 : 0;
        }
    }

    /** Pulses a redstone signal on a fixed interval; right-click to change the interval. */
    public static class Clock extends Block {
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
        public static final IntegerProperty DELAY = IntegerProperty.create("delay", 0, 4);
        private static final int[] TICKS = {5, 10, 20, 40, 100};

        public Clock(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(DELAY, 2));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(POWERED, DELAY);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            if (!old.is(this)) level.scheduleTick(pos, this, TICKS[state.getValue(DELAY)]);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (level.hasNeighborSignal(pos)) {
                level.scheduleTick(pos, this, 10);
                return;
            }
            boolean p = state.getValue(POWERED);
            level.setBlock(pos, state.setValue(POWERED, !p), 3);
            level.scheduleTick(pos, this, p ? TICKS[state.getValue(DELAY)] : 2);
        }

        @Override
        @SuppressWarnings("deprecation")
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                BlockState n = state.cycle(DELAY);
                level.setBlock(pos, n, 3);
                player.displayClientMessage(Component.translatable("msg.fw_redstone.clock", TICKS[n.getValue(DELAY)] / 20.0f), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
            return state.getValue(POWERED) ? 15 : 0;
        }
    }
}
