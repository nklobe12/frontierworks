package com.lodestar.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.Supplier;

/** Shaft coupling: joins the shafts on both ends until a redstone signal uncouples it. */
public class CouplingBlock extends KineticAxisBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public CouplingBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, float radius) {
        super(props, type, radius, false);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide) return;
        boolean p = level.hasNeighborSignal(pos);
        if (p != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, p), 3);
    }
}
