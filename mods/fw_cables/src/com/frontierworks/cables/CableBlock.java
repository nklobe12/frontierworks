package com.frontierworks.cables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Energy cable that connects to other cables and to anything with a Forge Energy capability. */
public class CableBlock extends Block implements EntityBlock {
    public static final Map<Direction, BooleanProperty> PROPS = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape core = Block.box(6, 6, 6, 10, 10, 10);
        VoxelShape[] arms = {
                Block.box(6, 0, 6, 10, 6, 10), Block.box(6, 10, 6, 10, 16, 10),
                Block.box(6, 6, 0, 10, 10, 6), Block.box(6, 6, 10, 10, 10, 16),
                Block.box(0, 6, 6, 6, 10, 10), Block.box(10, 6, 6, 16, 10, 10)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) s = Shapes.or(s, arms[i]);
            }
            SHAPES[mask] = s;
        }
    }

    public final int rate;
    private final Supplier<? extends BlockEntityType<?>> type;

    public CableBlock(Properties props, int rate, Supplier<? extends BlockEntityType<?>> type) {
        super(props);
        this.rate = rate;
        this.type = type;
        BlockState def = stateDefinition.any();
        for (BooleanProperty p : PROPS.values()) def = def.setValue(p, false);
        registerDefaultState(def);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPS.values().toArray(new BooleanProperty[0]));
    }

    public static boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        BlockState ns = level.getBlockState(np);
        if (ns.getBlock() instanceof CableBlock) return true;
        BlockEntity be = level.getBlockEntity(np);
        return be != null && !(be instanceof CableBE) && be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).isPresent();
    }

    private BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPS.get(d), canConnect(level, pos, d));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CableBE) ((CableBE) be).invalidateNetwork();
        return state.setValue(PROPS.get(dir), canConnect(level, pos, dir));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CableBE) ((CableBE) be).invalidateNetwork();
        BlockState updated = withConnections(level, pos, state);
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CableBE) ((CableBE) be).invalidateNetwork();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        Direction[] order = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int i = 0; i < 6; i++) {
            if (state.getValue(PROPS.get(order[i]))) mask |= 1 << i;
        }
        return SHAPES[mask];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_cables.rate", rate).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
