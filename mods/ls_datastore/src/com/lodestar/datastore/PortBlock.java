package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Importer, exporter or inventory link: faces the block it was placed against. */
public class PortBlock extends LSEntityBlock {
    public PortBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, Facing.ALL, false);
        noBuckets();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING_ALL, ctx.getClickedFace().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (old.is(this) && old.getValue(FACING_ALL) != state.getValue(FACING_ALL) && level.getBlockEntity(pos) instanceof PortBE be) {
            be.invalidateGrid();
            be.targetChanged();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.getBlockEntity(pos) instanceof PortBE be && from.equals(pos.relative(state.getValue(FACING_ALL)))) be.targetChanged();
    }
}
