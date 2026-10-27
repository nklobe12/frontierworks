package com.frontierworks.batteries;

import com.frontierworks.core.energy.EnergyUtil;
import com.frontierworks.core.machine.MachineBlock;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Energy cell: accepts power on every side but the front, outputs through the front. Keeps its charge when broken. */
public class BatteryBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(80, 17, 52)
            .slot(44, 35, MachineLayout.Role.BATTERY)
            .slot(116, 35, MachineLayout.Role.CHARGE)
            .info(8, 72).build();
    private final int rate;
    private final LazyOptional<IEnergyStorage> inputCap;
    private final LazyOptional<IEnergyStorage> outputCap;

    public BatteryBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int rate) {
        super(type, pos, state, LAYOUT, capacity, rate, rate);
        this.rate = rate;
        this.inputCap = LazyOptional.of(() -> new SidedEnergy(true));
        this.outputCap = LazyOptional.of(() -> new SidedEnergy(false));
    }

    private Direction front() {
        BlockState s = getBlockState();
        return s.hasProperty(MachineBlock.FACING) ? s.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        chargeItems(rate);
        Direction f = front();
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, rate, d -> d == f);
        setLit(energy.getEnergyStored() > 0);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null) {
            return side == front() ? outputCap.cast() : inputCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate();
        outputCap.invalidate();
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.fw_batteries.io", rate));
    }

    private class SidedEnergy implements IEnergyStorage {
        private final boolean input;

        SidedEnergy(boolean input) {
            this.input = input;
        }

        @Override
        public int receiveEnergy(int max, boolean simulate) {
            return input && energy != null ? energy.receiveEnergy(max, simulate) : 0;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return !input && energy != null ? energy.extractEnergy(max, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            return energy == null ? 0 : energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy == null ? 0 : energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return !input;
        }

        @Override
        public boolean canReceive() {
            return input;
        }
    }
}
