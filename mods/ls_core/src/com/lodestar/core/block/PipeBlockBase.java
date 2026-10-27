package com.lodestar.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

/** Six-way connecting pipe with a ticking block entity hook. Subclasses decide what they connect to. */
public abstract class PipeBlockBase extends Block implements EntityBlock {
    public static final Map<Direction, BooleanProperty> PROPS = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final Direction[] ORDER = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private final VoxelShape[] shapes = new VoxelShape[64];
    protected final Supplier<? extends BlockEntityType<?>> type;

    protected PipeBlockBase(Properties props, float radius, Supplier<? extends BlockEntityType<?>> type) {
        super(props);
        this.type = type;
        double a = 8 - radius, b = 8 + radius;
        VoxelShape core = Block.box(a, a, a, b, b, b);
        VoxelShape[] arms = {Block.box(a, 0, a, b, a, b), Block.box(a, b, a, b, 16, b), Block.box(a, a, 0, b, b, a),
                Block.box(a, a, b, b, b, 16), Block.box(0, a, a, a, b, b), Block.box(b, a, a, 16, b, b)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0) s = Shapes.or(s, arms[i]);
            shapes[mask] = s;
        }
        BlockState def = stateDefinition.any();
        for (BooleanProperty p : PROPS.values()) def = def.setValue(p, false);
        registerDefaultState(def);
    }

    /** Whether this pipe should visually and logically connect towards dir. */
    public abstract boolean canConnect(BlockGetter level, BlockPos pos, Direction dir);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPS.values().toArray(new BooleanProperty[0]));
    }

    protected BlockState connections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) state = state.setValue(PROPS.get(d), canConnect(level, pos, d));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        onNetworkChange(level, pos);
        return state.setValue(PROPS.get(dir), canConnect(level, pos, dir));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        onNetworkChange(level, pos);
        BlockState updated = connections(level, pos, state);
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    protected void onNetworkChange(LevelAccessor level, BlockPos pos) {}

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (int i = 0; i < 6; i++) if (state.getValue(PROPS.get(ORDER[i]))) mask |= 1 << i;
        return shapes[mask];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide || (Object) t != (Object) type.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof Ticking) ((Ticking) be).serverTick();
        };
    }

    /** Implemented by pipe block entities that need a server tick. */
    public interface Ticking {
        void serverTick();
    }
}
