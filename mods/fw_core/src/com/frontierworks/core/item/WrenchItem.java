package com.frontierworks.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

/** Engineer's Wrench: right-click rotates blocks, sneak-right-click dismantles Frontierworks blocks. */
public class WrenchItem extends Item {
    public WrenchItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (player != null && player.isShiftKeyDown()) {
            if (id != null && id.getNamespace().startsWith("fw_") && state.getDestroySpeed(level, pos) >= 0) {
                if (!level.isClientSide) {
                    level.destroyBlock(pos, true, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
        BlockState rotated = rotate(state, ctx.getClickedFace());
        if (rotated != null && rotated != state && rotated.canSurvive(level, pos)) {
            if (!level.isClientSide) {
                level.setBlock(pos, rotated, Block.UPDATE_ALL);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static BlockState rotate(BlockState state, Direction face) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return cycle(state, BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return cycle(state, BlockStateProperties.AXIS);
        }
        return null;
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> prop) {
        return state.cycle(prop);
    }
}
