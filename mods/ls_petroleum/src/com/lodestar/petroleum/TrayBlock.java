package com.lodestar.petroleum;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.function.Supplier;

/** A distillation tray; FRACTION shows which fraction it draws off (0 while not part of a column). */
public class TrayBlock extends LSEntityBlock {
    public static final IntegerProperty FRACTION = IntegerProperty.create("fraction", 0, 5);

    public TrayBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type);
        noMenu();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRACTION);
    }
}
