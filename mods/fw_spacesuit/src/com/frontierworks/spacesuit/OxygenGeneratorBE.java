package com.frontierworks.spacesuit;

import com.frontierworks.core.energy.FWEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Turns energy into oxygen; players within 8 blocks can breathe and refill their suits from the tank. */
public class OxygenGeneratorBE extends BlockEntity {
    public static final Set<OxygenGeneratorBE> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
    public static final int MAX_OXYGEN = 24000;
    public static final int COST = 40;
    private final FWEnergyStorage energy = new FWEnergyStorage(100_000, 2_000, 0, this::setChanged);
    private final LazyOptional<IEnergyStorage> energyOpt = LazyOptional.of(() -> energy);
    private int oxygen;

    public OxygenGeneratorBE(BlockPos pos, BlockState state) {
        super(Content.OXYGEN_GENERATOR_BE.get(), pos, state);
    }

    public int oxygen() {
        return oxygen;
    }

    public int energy() {
        return energy.getEnergyStored();
    }

    void serverTick() {
        LOADED.add(this);
        if (oxygen < MAX_OXYGEN && energy.getEnergyStored() >= COST) {
            energy.consume(COST);
            oxygen = Math.min(MAX_OXYGEN, oxygen + 1);
            setChanged();
        }
        boolean lit = oxygen > 0;
        BlockState st = getBlockState();
        if (st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT) != lit && level != null) {
            level.setBlock(worldPosition, st.setValue(BlockStateProperties.LIT, lit), 3);
        }
    }

    /** One player breathing for one second. */
    public boolean breathe() {
        if (oxygen <= 0) return false;
        oxygen--;
        setChanged();
        return true;
    }

    public int take(int amount) {
        int n = Math.min(amount, oxygen);
        oxygen -= n;
        setChanged();
        return n;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        energy.save(tag, "Energy");
        tag.putInt("Oxygen", oxygen);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.load(tag, "Energy");
        oxygen = tag.getInt("Oxygen");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOpt.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }
}
