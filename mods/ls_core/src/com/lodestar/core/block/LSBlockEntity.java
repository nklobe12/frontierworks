package com.lodestar.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Base block entity for the pack. Separates full persistence ({@link #saveAll}/{@link #loadAll}) from the
 * smaller client view ({@link #saveSync}/{@link #loadSync}) and throttles client updates to one per tick.
 * Subclasses implement {@link #tickServer()} / {@link #tickClient()} and are ticked by {@link LSEntityBlock}.
 */
public abstract class LSBlockEntity extends BlockEntity {
    private boolean syncPending;
    private long lastSync = -1;

    protected LSBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickServer() {}

    public void tickClient() {}

    /** Called by the ticker every server tick before {@link #tickServer()}; flushes pending syncs. */
    public final void baseServerTick() {
        tickServer();
        if (syncPending && level != null && level.getGameTime() != lastSync) {
            syncPending = false;
            lastSync = level.getGameTime();
            BlockState st = getBlockState();
            level.sendBlockUpdated(worldPosition, st, st, Block.UPDATE_CLIENTS);
        }
    }

    /** Requests a client update (sent at most once per tick). Also marks the chunk dirty. */
    public void sync() {
        setChanged();
        syncPending = true;
        if (level != null && !level.isClientSide && !isTicking()) {
            syncPending = false;
            BlockState st = getBlockState();
            level.sendBlockUpdated(worldPosition, st, st, Block.UPDATE_CLIENTS);
        }
    }

    /** Whether this block entity has a server ticker that flushes pending syncs. */
    protected boolean isTicking() {
        return true;
    }

    // ------------------------------------------------------------------ persistence

    protected void saveAll(CompoundTag tag) {}

    protected void loadAll(CompoundTag tag) {}

    /** Data the client needs (rendering, screens opened without menus). Defaults to everything. */
    protected void saveSync(CompoundTag tag) {
        saveAll(tag);
    }

    protected void loadSync(CompoundTag tag) {
        loadAll(tag);
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveAll(tag);
    }

    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        loadAll(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveSync(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        loadSync(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadSync(tag);
        }
    }

    public boolean isServer() {
        return level != null && !level.isClientSide;
    }
}
