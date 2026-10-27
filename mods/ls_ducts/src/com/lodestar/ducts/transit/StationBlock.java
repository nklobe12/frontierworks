package com.lodestar.ducts.transit;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** Transit station: where pods start and stop. Joins viaducts on every side. */
public class StationBlock extends LSEntityBlock {
    public StationBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, Facing.NONE, false);
        noMenu();
        noBuckets();
    }
}
