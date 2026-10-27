package com.frontierworks.cables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cable block entity. Cables hold no energy: anything pushed into a cable is immediately distributed
 * across the whole connected network to every energy receiver, limited by the slowest cable's rate.
 */
public class CableBE extends BlockEntity {
    private Network network;
    private final LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> new Entry(this));

    public CableBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void invalidateNetwork() {
        if (network != null) network.valid = false;
        network = null;
    }

    Network network() {
        if (network == null || !network.valid) {
            network = Network.build(level, worldPosition);
        }
        return network;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> c, @Nullable Direction side) {
        if (c == ForgeCapabilities.ENERGY) return cap.cast();
        return super.getCapability(c, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateNetwork();
    }

    static class Target {
        final BlockPos pos;
        final Direction side;

        Target(BlockPos pos, Direction side) {
            this.pos = pos;
            this.side = side;
        }
    }

    static class Network {
        boolean valid = true;
        final List<Target> targets = new ArrayList<>();
        int rate = Integer.MAX_VALUE;
        long tick = -1;
        int moved;
        int rr;

        static Network build(Level level, BlockPos start) {
            Network n = new Network();
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            while (!queue.isEmpty() && seen.size() < 4096) {
                BlockPos p = queue.poll();
                BlockEntity be = level.getBlockEntity(p);
                if (!(be instanceof CableBE)) continue;
                ((CableBE) be).network = n;
                BlockState st = be.getBlockState();
                if (st.getBlock() instanceof CableBlock) n.rate = Math.min(n.rate, ((CableBlock) st.getBlock()).rate);
                for (Direction d : Direction.values()) {
                    BlockPos np = p.relative(d);
                    if (!level.isLoaded(np)) continue;
                    BlockEntity nbe = level.getBlockEntity(np);
                    if (nbe instanceof CableBE) {
                        if (seen.add(np)) queue.add(np);
                    } else if (nbe != null && nbe.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).isPresent()) {
                        n.targets.add(new Target(np, d.getOpposite()));
                    }
                }
            }
            if (n.rate == Integer.MAX_VALUE) n.rate = 256;
            return n;
        }

        int distribute(Level level, int amount, boolean simulate, @Nullable BlockPos except) {
            long now = level.getGameTime();
            if (now != tick) {
                tick = now;
                moved = 0;
            }
            int budget = Math.min(amount, rate - moved);
            if (budget <= 0 || targets.isEmpty()) return 0;
            int sent = 0;
            int count = targets.size();
            for (int i = 0; i < count && sent < budget; i++) {
                Target t = targets.get((rr + i) % count);
                if (t.pos.equals(except)) continue;
                BlockEntity be = level.getBlockEntity(t.pos);
                if (be == null) continue;
                IEnergyStorage es = be.getCapability(ForgeCapabilities.ENERGY, t.side).orElse(null);
                if (es == null || !es.canReceive()) continue;
                int share = Math.max(1, (budget - sent) / Math.max(1, count - i));
                sent += es.receiveEnergy(share, simulate);
            }
            // second pass: give leftovers to anyone still accepting
            for (int i = 0; i < count && sent < budget; i++) {
                Target t = targets.get((rr + i) % count);
                if (t.pos.equals(except)) continue;
                BlockEntity be = level.getBlockEntity(t.pos);
                if (be == null) continue;
                IEnergyStorage es = be.getCapability(ForgeCapabilities.ENERGY, t.side).orElse(null);
                if (es == null || !es.canReceive()) continue;
                sent += es.receiveEnergy(budget - sent, simulate);
            }
            if (!simulate) {
                moved += sent;
                rr = (rr + 1) % Math.max(1, count);
            }
            return sent;
        }
    }

    /** What neighbours see: a receiver that forwards into the network. */
    static class Entry implements IEnergyStorage {
        private final CableBE cable;

        Entry(CableBE cable) {
            this.cable = cable;
        }

        @Override
        public int receiveEnergy(int max, boolean simulate) {
            if (cable.level == null || cable.level.isClientSide) return 0;
            return cable.network().distribute(cable.level, max, simulate, null);
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return cable.level == null ? 0 : cable.network().rate;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
