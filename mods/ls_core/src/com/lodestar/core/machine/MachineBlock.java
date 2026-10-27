package com.lodestar.core.machine;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** Horizontal-facing machine block with a LIT state that opens the shared machine screen. */
public class MachineBlock extends LSEntityBlock {
    public MachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, Facing.HORIZONTAL, true);
    }

    @Override
    public MachineBlock tooltip(String key) {
        super.tooltip(key);
        return this;
    }
}
