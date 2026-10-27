package com.lodestar.dynamos;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Base of every dynamo: burns some fuel into an energy budget and releases it at a steady rate, pushing power out
 * of its coil (the face it points at) only. Output augments raise the rate, efficiency augments stretch the fuel.
 */
public abstract class DynamoBE extends MachineBlockEntity {
    protected final int baseRate;
    /** energy left from the current fuel */
    protected long fuelEnergy;
    protected long fuelEnergyMax;

    protected DynamoBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout, int baseRate) {
        super(type, pos, state, layout, baseRate * 400, 0, baseRate * 4);
        this.baseRate = baseRate;
    }

    @Override
    public boolean hasSideConfig() {
        return false;
    }

    @Override
    public boolean acceptsAugment(AugmentItem a) {
        return a.kind == AugmentItem.Kind.OUTPUT || a.kind == AugmentItem.Kind.EFFICIENCY || a.kind == AugmentItem.Kind.CAPACITY
                || (a.kind == AugmentItem.Kind.FLUID && tanks.length > 0);
    }

    /** Takes the next unit of fuel; returns the energy it is worth (0 when there is none). */
    protected abstract long consumeFuel();

    /** Production rate this tick before augments (some dynamos vary). */
    protected int currentRate() {
        return baseRate;
    }

    public Direction coil() {
        return LSEntityBlock.facingOf(getBlockState());
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        int made = 0;
        if (canRun() && !energy.isFull()) {
            if (fuelEnergy <= 0) {
                long e = consumeFuel();
                if (e > 0) {
                    fuelEnergy = fuelEnergyMax = Math.round(e * (1 + augment(AugmentItem.Kind.EFFICIENCY)));
                }
            }
            if (fuelEnergy > 0) {
                int r = Math.round(currentRate() * (1 + outputBonus()));
                made = (int) Math.min(r, Math.min(fuelEnergy, energy.space()));
                energy.produce(made);
                fuelEnergy -= made;
            }
        }
        rate = made;
        burn = (int) Math.min(Integer.MAX_VALUE, fuelEnergy);
        maxBurn = (int) Math.min(Integer.MAX_VALUE, Math.max(1, fuelEnergyMax));
        push();
        chargeItems(baseRate * 4);
        setLit(made > 0);
        setChanged();
    }

    private void push() {
        if (energy.getEnergyStored() <= 0) return;
        Direction d = coil();
        BlockEntity other = level.getBlockEntity(worldPosition.relative(d));
        if (other == null) return;
        other.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).ifPresent(dest -> {
            int give = Math.min(energy.getEnergyStored(), Math.max(baseRate * 4, rate * 2));
            int accepted = dest.receiveEnergy(give, false);
            if (accepted > 0) energy.consume(accepted);
        });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null && side != coil()) return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override
    protected IEnergyStorage getEnergyForCap() {
        return new IEnergyStorage() {
            public int receiveEnergy(int max, boolean sim) {
                return 0;
            }

            public int extractEnergy(int max, boolean sim) {
                return energy.extractEnergy(max, sim);
            }

            public int getEnergyStored() {
                return energy.getEnergyStored();
            }

            public int getMaxEnergyStored() {
                return energy.getMaxEnergyStored();
            }

            public boolean canExtract() {
                return true;
            }

            public boolean canReceive() {
                return false;
            }
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_core.generating", menu.data(D_RATE)));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putLong("Fuel", fuelEnergy);
        tag.putLong("FuelMax", fuelEnergyMax);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        fuelEnergy = tag.getLong("Fuel");
        fuelEnergyMax = tag.getLong("FuelMax");
    }
}
