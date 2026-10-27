package com.lodestar.toolsmith.foundry;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.multiblock.IMultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Foundry tank: holds 4 buckets of any fluid. In a foundry wall, its lava is the foundry's fuel. */
public class FoundryTankBE extends LSBlockEntity implements IMultiblockPart {
    public final FluidTank tank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() {
            sync();
        }
    };
    private final LazyOptional<IFluidHandler> cap = LazyOptional.of(() -> tank);
    @Nullable
    private BlockPos controller;

    public FoundryTankBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static boolean isFuel(FluidStack f) {
        return f.getFluid().is(FluidTags.LAVA);
    }

    @Override
    protected boolean isTicking() {
        return false;
    }

    @Override
    public void setController(@Nullable BlockPos c) {
        controller = c;
        setChanged();
    }

    @Nullable
    @Override
    public BlockPos getController() {
        return controller;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> c, @Nullable Direction side) {
        if (c == ForgeCapabilities.FLUID_HANDLER) return cap.cast();
        return super.getCapability(c, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        if (controller != null) tag.put("Ctrl", NbtUtils.writeBlockPos(controller));
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        tank.readFromNBT(tag.getCompound("Tank"));
        controller = tag.contains("Ctrl") ? NbtUtils.readBlockPos(tag.getCompound("Ctrl")) : null;
    }
}
