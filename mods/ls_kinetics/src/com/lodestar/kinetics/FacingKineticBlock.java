package com.lodestar.kinetics;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * A kinetic block facing any direction, with its shape given as boxes for the "facing up" model and rotated to match
 * the block state. clickedFace: face the clicked face (mounted parts like hand wheels) instead of the player.
 */
public class FacingKineticBlock extends LSEntityBlock {
    private final VoxelShape[] shapes = new VoxelShape[6];
    private final boolean clickedFace;

    public FacingKineticBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, boolean clickedFace, double[]... upBoxes) {
        super(props, type, Facing.ALL, false);
        this.clickedFace = clickedFace;
        for (Direction d : Direction.values()) {
            VoxelShape s = Shapes.empty();
            for (double[] b : upBoxes) {
                double[] r = rotateBox(b, d);
                s = Shapes.or(s, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
            }
            shapes[d.get3DDataValue()] = upBoxes.length == 0 ? Shapes.block() : s;
        }
        noMenu();
        noBuckets();
    }

    /** Box (x0,y0,z0,x1,y1,z1 in pixels) of a model facing up, rotated so the model faces d (blockstate x/y rotations). */
    public static double[] rotateBox(double[] b, Direction d) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (d) {
            case UP -> b.clone();
            case DOWN -> new double[]{x0, 16 - y1, 16 - z1, x1, 16 - y0, 16 - z0};
            case NORTH -> new double[]{x0, z0, 16 - y1, x1, z1, 16 - y0};
            case SOUTH -> new double[]{16 - x1, z0, y0, 16 - x0, z1, y1};
            case EAST -> new double[]{y0, z0, x0, y1, z1, x1};
            case WEST -> new double[]{16 - y1, z0, 16 - x1, 16 - y0, z1, 16 - x0};
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (clickedFace) return defaultBlockState().setValue(FACING_ALL, ctx.getClickedFace());
        return super.getStateForPlacement(ctx);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(FACING_ALL).get3DDataValue()];
    }
}
