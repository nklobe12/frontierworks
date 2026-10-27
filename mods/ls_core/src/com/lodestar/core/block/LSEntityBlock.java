package com.lodestar.core.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * General block with a block entity: optional horizontal FACING and LIT states, ticking through
 * {@link LSBlockEntity}, opening a menu when the block entity is a {@link MenuProvider}, bucket interaction
 * with fluid handlers, dropping {@link IDropContents} inventories and comparator output.
 */
public class LSEntityBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final DirectionProperty FACING_ALL = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** How the block faces when placed. */
    public enum Facing { NONE, HORIZONTAL, ALL }

    protected final Supplier<? extends BlockEntityType<?>> type;
    protected final Facing facing;
    protected final boolean lit;
    @Nullable
    protected VoxelShape shape;
    @Nullable
    protected String tooltipKey;
    protected boolean openMenu = true;
    protected boolean bucketInteract = true;

    public LSEntityBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, Facing facing, boolean lit) {
        super(prep(props, facing, lit));
        PENDING.remove();
        this.type = type;
        this.facing = facing;
        this.lit = lit;
        BlockState def = stateDefinition.any();
        if (facing == Facing.HORIZONTAL) def = def.setValue(FACING, Direction.NORTH);
        if (facing == Facing.ALL) def = def.setValue(FACING_ALL, Direction.NORTH);
        if (lit) def = def.setValue(LIT, false);
        registerDefaultState(def);
    }

    public LSEntityBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        this(props, type, Facing.NONE, false);
    }

    public LSEntityBlock shape(VoxelShape s) {
        this.shape = s;
        return this;
    }

    public LSEntityBlock tooltip(String key) {
        this.tooltipKey = key;
        return this;
    }

    public LSEntityBlock noMenu() {
        this.openMenu = false;
        return this;
    }

    public LSEntityBlock noBuckets() {
        this.bucketInteract = false;
        return this;
    }

    // Block's constructor builds the state definition before our fields are assigned, so the wanted
    // properties are passed through a thread-local set while evaluating the super() arguments.
    private static final ThreadLocal<PropertyHolder> PENDING = new ThreadLocal<>();

    private record PropertyHolder(Facing facing, boolean lit) {}

    private static Properties prep(Properties props, Facing facing, boolean lit) {
        PENDING.set(new PropertyHolder(facing, lit));
        return props;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PropertyHolder h = PENDING.get();
        if (h != null) {
            if (h.facing == Facing.HORIZONTAL) builder.add(FACING);
            if (h.facing == Facing.ALL) builder.add(FACING_ALL);
            if (h.lit) builder.add(LIT);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        if (facing == Facing.HORIZONTAL) s = s.setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        if (facing == Facing.ALL) s = s.setValue(FACING_ALL, ctx.getNearestLookingDirection().getOpposite());
        return s;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (facing == Facing.HORIZONTAL) return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
        if (facing == Facing.ALL) return state.setValue(FACING_ALL, rot.rotate(state.getValue(FACING_ALL)));
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (facing == Facing.HORIZONTAL) return state.rotate(mirror.getRotation(state.getValue(FACING)));
        if (facing == Facing.ALL) return state.rotate(mirror.getRotation(state.getValue(FACING_ALL)));
        return state;
    }

    public static Direction facingOf(BlockState state) {
        if (state.hasProperty(FACING)) return state.getValue(FACING);
        if (state.hasProperty(FACING_ALL)) return state.getValue(FACING_ALL);
        return Direction.NORTH;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape != null ? shape : super.getShape(state, level, pos, ctx);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if ((Object) t != (Object) type.get()) return null;
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof LSBlockEntity l) l.tickClient();
            };
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof LSBlockEntity l) l.baseServerTick();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return InteractionResult.PASS;
        if (be instanceof IInteractable ia) {
            InteractionResult r = ia.onUse(player, hand, hit);
            if (r != InteractionResult.PASS) return r;
        }
        if (bucketInteract && FluidUtil.getFluidHandler(level, pos, hit.getDirection()).isPresent()
                && FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (openMenu && be instanceof MenuProvider mp) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, mp, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IPlacedBy p) {
            p.onPlacedBy(placer, stack);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IDropContents d) {
                d.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof INeighborAware n) {
            n.onNeighborChanged(from);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IComparatorOutput c) return c.comparatorOutput();
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (tooltipKey != null) {
            for (String line : Component.translatable(tooltipKey).getString().split("\n")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    // ------------------------------------------------------------------ optional block entity hooks

    public interface IDropContents {
        void dropContents(Level level, BlockPos pos);

        static void dropHandler(Level level, BlockPos pos, IItemHandler h) {
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack st = h.getStackInSlot(i);
                if (!st.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st.copy());
            }
        }
    }

    public interface IInteractable {
        InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit);
    }

    public interface IPlacedBy {
        void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack);
    }

    public interface INeighborAware {
        void onNeighborChanged(BlockPos from);
    }

    public interface IComparatorOutput {
        int comparatorOutput();
    }
}
