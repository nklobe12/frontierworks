package com.lodestar.lapidary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A gem crystal growing out of a budding block, in four stages (small, medium and large bud, full cluster). Only a full
 * cluster yields rough gems. Attached to the face it grows from; waterloggable; glows faintly as it grows.
 */
public class GemClusterBlock extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int[][] SIZE = {{3, 4}, {4, 3}, {5, 3}, {7, 3}};
    private final VoxelShape[][] shapes = new VoxelShape[4][6];

    public GemClusterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 3).setValue(FACING, Direction.UP).setValue(WATERLOGGED, false));
        for (int s = 0; s < 4; s++) {
            int h = SIZE[s][0], o = SIZE[s][1];
            shapes[s][Direction.UP.ordinal()] = Block.box(o, 0, o, 16 - o, h, 16 - o);
            shapes[s][Direction.DOWN.ordinal()] = Block.box(o, 16 - h, o, 16 - o, 16, 16 - o);
            shapes[s][Direction.NORTH.ordinal()] = Block.box(o, o, 16 - h, 16 - o, 16 - o, 16);
            shapes[s][Direction.SOUTH.ordinal()] = Block.box(o, o, 0, 16 - o, 16 - o, h);
            shapes[s][Direction.EAST.ordinal()] = Block.box(0, o, o, h, 16 - o, 16 - o);
            shapes[s][Direction.WEST.ordinal()] = Block.box(16 - h, o, o, 16, 16 - o, 16 - o);
        }
    }

    public static Properties props(MapColor color) {
        return Properties.of().mapColor(color).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER).strength(1.5f)
                .pushReaction(PushReaction.DESTROY).lightLevel(s -> s.getValue(STAGE) + 2);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE, FACING, WATERLOGGED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(STAGE)][state.getValue(FACING).ordinal()];
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction d = state.getValue(FACING);
        BlockPos base = pos.relative(d.getOpposite());
        return level.getBlockState(base).isFaceSturdy(level, base, d);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (dir == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        LevelAccessor level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(FACING, ctx.getClickedFace());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
