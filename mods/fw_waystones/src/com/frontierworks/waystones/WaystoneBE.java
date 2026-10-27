package com.frontierworks.waystones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the waystone's name, generated from its position the first time it is needed. */
public class WaystoneBE extends BlockEntity {
    private static final String[] A = {"Amber", "Ash", "Bright", "Cinder", "Cold", "Dawn", "Deep", "Elder", "Ember", "Fair", "Frost", "Gold",
            "Gray", "High", "Iron", "Kings", "Long", "Moon", "Oak", "Old", "Raven", "Red", "Salt", "Silver", "Star", "Stone", "Storm", "Sun",
            "Thorn", "West", "Wild", "Wolf"};
    private static final String[] B = {"brook", "crest", "dale", "fall", "fell", "ford", "gate", "glen", "haven", "hold", "hollow", "keep",
            "march", "mere", "moor", "peak", "reach", "rest", "ridge", "rock", "shore", "spire", "stead", "vale", "watch", "well", "wick", "wood"};
    private String name = "";

    public WaystoneBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String getName() {
        if (name.isEmpty()) {
            long h = worldPosition.asLong() * 0x9E3779B97F4A7C15L;
            name = A[(int) Math.floorMod(h >>> 7, (long) A.length)] + B[(int) Math.floorMod(h >>> 23, (long) B.length)];
            setChanged();
        }
        return name;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("WaystoneName", name);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        name = tag.getString("WaystoneName");
    }
}
