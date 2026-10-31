package com.lodestar.petroleum;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.multiblock.IMultiblockController;
import com.lodestar.core.multiblock.IMultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A tray of a distillation column: lets pipes and buckets draw its fraction from the column's reboiler. */
public class TrayBE extends LSBlockEntity implements IMultiblockPart {
    @Nullable
    private BlockPos controller;

    public TrayBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean isTicking() {
        return false;
    }

    @Override
    public void setController(@Nullable BlockPos c) {
        if (c == null ? controller != null : !c.equals(controller)) {
            controller = c;
            invalidateCaps();
            reviveCaps();
            setChanged();
            if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Nullable
    @Override
    public BlockPos getController() {
        return controller;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (controller != null && level != null && level.isLoaded(controller) && level.getBlockEntity(controller) instanceof IMultiblockController c) {
            LazyOptional<T> o = c.getPortCapability(cap, worldPosition, side);
            if (o.isPresent()) return o;
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        if (controller != null) tag.put("Ctrl", NbtUtils.writeBlockPos(controller));
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        controller = tag.contains("Ctrl") ? NbtUtils.readBlockPos(tag.getCompound("Ctrl")) : null;
    }
}
