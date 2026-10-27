package com.lodestar.ducts.transit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** Glass transit tube. Joins other tubes and transit stations; pods carry players through it. */
public class ViaductBlock extends Block {
    public static final Map<Direction, BooleanProperty> PROPS = PipeBlock.PROPERTY_BY_DIRECTION;
    private final VoxelShape[] shapes = new VoxelShape[64];

    public ViaductBlock(Properties props) {
        super(props.noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
        double a = 3, b = 13;
        VoxelShape core = Block.box(a, a, a, b, b, b);
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.get3DDataValue())) == 0) continue;
                s = Shapes.or(s, switch (d) {
                    case DOWN -> Block.box(a, 0, a, b, a, b);
                    case UP -> Block.box(a, b, a, b, 16, b);
                    case NORTH -> Block.box(a, a, 0, b, b, a);
                    case SOUTH -> Block.box(a, a, b, b, b, 16);
                    case WEST -> Block.box(0, a, a, a, b, b);
                    case EAST -> Block.box(b, a, a, 16, b, b);
                });
            }
            shapes[mask] = s;
        }
        BlockState def = stateDefinition.any();
        for (BooleanProperty p : PROPS.values()) def = def.setValue(p, false);
        registerDefaultState(def);
    }

    public static boolean joins(BlockState s) {
        return s.getBlock() instanceof ViaductBlock || s.getBlock() instanceof StationBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PROPS.values().toArray(new BooleanProperty[0]));
    }

    private BlockState connect(BlockGetter level, BlockPos pos, BlockState s) {
        for (Direction d : Direction.values()) s = s.setValue(PROPS.get(d), joins(level.getBlockState(pos.relative(d))));
        return s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connect(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return state.setValue(PROPS.get(dir), joins(other));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction d : Direction.values()) if (state.getValue(PROPS.get(d))) mask |= 1 << d.get3DDataValue();
        return shapes[mask];
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
