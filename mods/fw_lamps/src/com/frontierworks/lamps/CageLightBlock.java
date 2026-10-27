package com.frontierworks.lamps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small caged lamp that mounts on floors, ceilings and walls. */
public class CageLightBlock extends Block {
    public enum Mount implements StringRepresentable {
        FLOOR("floor", Direction.DOWN), CEILING("ceiling", Direction.UP), WALL_NORTH("wall_north", Direction.NORTH),
        WALL_SOUTH("wall_south", Direction.SOUTH), WALL_WEST("wall_west", Direction.WEST), WALL_EAST("wall_east", Direction.EAST);

        final String name;
        final Direction attached;

        Mount(String name, Direction attached) {
            this.name = name;
            this.attached = attached;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static Mount of(Direction attachedTo) {
            for (Mount m : values()) if (m.attached == attachedTo) return m;
            return FLOOR;
        }
    }

    public static final EnumProperty<Mount> FACE = EnumProperty.create("face", Mount.class);
    private static final VoxelShape FLOOR = Block.box(4, 0, 4, 12, 9, 12);
    private static final VoxelShape CEIL = Block.box(4, 7, 4, 12, 16, 12);
    private static final VoxelShape N = Block.box(4, 4, 0, 12, 12, 9);
    private static final VoxelShape S = Block.box(4, 4, 7, 12, 12, 16);
    private static final VoxelShape W = Block.box(0, 4, 4, 9, 12, 12);
    private static final VoxelShape E = Block.box(7, 4, 4, 16, 12, 12);

    public CageLightBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACE, Mount.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // clicked face's opposite is the block we attach to
        return defaultBlockState().setValue(FACE, Mount.of(ctx.getClickedFace().getOpposite()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction d = state.getValue(FACE).attached;
        BlockPos support = pos.relative(d);
        return level.getBlockState(support).isFaceSturdy(level, support, d.getOpposite()) || d == Direction.UP;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos npos) {
        return dir == state.getValue(FACE).attached && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, npos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        switch (state.getValue(FACE)) {
            case CEILING: return CEIL;
            case WALL_NORTH: return N;
            case WALL_SOUTH: return S;
            case WALL_WEST: return W;
            case WALL_EAST: return E;
            default: return FLOOR;
        }
    }
}
