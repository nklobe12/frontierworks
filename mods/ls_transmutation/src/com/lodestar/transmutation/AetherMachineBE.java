package com.lodestar.transmutation;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared base of collectors, relays and condensers: an aether buffer and pushing aether to neighbours. */
public abstract class AetherMachineBE extends MachineBlockEntity implements IAetherHandler {
    protected long aether;
    protected final long capacity;
    protected final long transfer;

    protected AetherMachineBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout, long capacity, long transfer) {
        super(type, pos, state, layout, 0, 0, 0);
        this.capacity = capacity;
        this.transfer = transfer;
    }

    @Override
    public boolean hasSideConfig() {
        return false;
    }

    @Override
    public long aether() {
        return aether;
    }

    @Override
    public long aetherCapacity() {
        return capacity;
    }

    @Override
    public long receiveAether(long amount, boolean simulate) {
        long put = Math.min(amount, capacity - aether);
        if (put <= 0) return 0;
        if (!simulate) {
            aether += put;
            setChanged();
        }
        return put;
    }

    /** Pushes aether to neighbouring handlers that don't provide aether themselves (or relays, from collectors). */
    protected void pushAether(boolean toRelays) {
        if (aether <= 0 || level == null) return;
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(d));
            if (!(be instanceof IAetherHandler h) || be == this) continue;
            if (h.providesAether() && !(toRelays && be instanceof RelayBE)) continue;
            long give = Math.min(transfer, aether);
            long taken = h.receiveAether(give, false);
            aether -= taken;
            if (aether <= 0) break;
        }
    }

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case 0 -> (int) Math.min(Integer.MAX_VALUE, aether);
            case 1 -> (int) Math.min(Integer.MAX_VALUE, capacity);
            default -> 0;
        };
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putLong("Aether", aether);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        aether = Math.min(capacity, tag.getLong("Aether"));
    }
}
