package com.lodestar.kinetics;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A kinetic part lying along an axis (axles, spur gears, pulleys, gauges, paddle wheels). Placed along the axis of the
 * clicked face; horizontal-only parts (paddle wheels) lie across the direction the player faces. The wrench turns it.
 */
public class KineticAxisBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    protected final Supplier<? extends BlockEntityType<?>> type;
    private final VoxelShape[] shapes = new VoxelShape[3];
    private final boolean horizontalOnly;

    /** radius: half width of the selection box in pixels (8 = full block). */
    public KineticAxisBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, float radius, boolean horizontalOnly) {
        super(props);
        this.type = type;
        this.horizontalOnly = horizontalOnly;
        double a = 8 - radius, b = 8 + radius;
        shapes[0] = Block.box(0, a, a, 16, b, b);
        shapes[1] = Block.box(a, 0, a, b, 16, b);
        shapes[2] = Block.box(a, a, 0, b, b, 16);
        registerDefaultState(stateDefinition.any().setValue(AXIS, horizontalOnly ? Direction.Axis.X : Direction.Axis.Y));
    }

    public KineticAxisBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, float radius) {
        this(props, type, radius, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction.Axis axis = horizontalOnly ? ctx.getHorizontalDirection().getClockWise().getAxis() : ctx.getClickedFace().getAxis();
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        Direction.Axis a = state.getValue(AXIS);
        if (a != Direction.Axis.Y && (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90)) {
            return state.setValue(AXIS, a == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return !horizontalOnly || state.getValue(AXIS) != Direction.Axis.Y;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(AXIS).ordinal()];
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
        if (level.isClientSide) return (l, p, s, be) -> {
            if (be instanceof LSBlockEntity b) b.tickClient();
        };
        return (l, p, s, be) -> {
            if (be instanceof LSBlockEntity b) b.baseServerTick();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof LSEntityBlock.IInteractable ia) return ia.onUse(player, hand, hit);
        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.getBlockEntity(pos) instanceof LSEntityBlock.INeighborAware n) n.onNeighborChanged(from);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LSEntityBlock.IDropContents d) d.dropContents(level, pos);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LSEntityBlock.IComparatorOutput c ? c.comparatorOutput() : 0;
    }
}
