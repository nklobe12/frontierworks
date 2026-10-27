package com.lodestar.toolsmith.foundry;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/** Faucet: attaches to the side of the block it is placed against and pours downwards. */
public class FaucetBlock extends LSEntityBlock {
    private static final VoxelShape N = Block.box(4, 4, 10, 12, 10, 16), S = Block.box(4, 4, 0, 12, 10, 6),
            E = Block.box(0, 4, 4, 6, 10, 12), W = Block.box(10, 4, 4, 16, 10, 12);

    public FaucetBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, Facing.HORIZONTAL, false);
        noMenu();
        noBuckets();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, d);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> N;
            case SOUTH -> S;
            case EAST -> E;
            default -> W;
        };
    }
}
