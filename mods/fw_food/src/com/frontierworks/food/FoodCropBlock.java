package com.frontierworks.food;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

/** Eight-stage crop whose seed item is supplied lazily (the seeds are registered after the block). */
public class FoodCropBlock extends CropBlock {
    private final Supplier<? extends Item> seeds;

    public FoodCropBlock(Properties props, Supplier<? extends Item> seeds) {
        super(props);
        this.seeds = seeds;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }
}
