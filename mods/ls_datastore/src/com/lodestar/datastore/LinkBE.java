package com.lodestar.datastore;

import com.lodestar.datastore.storage.HandlerStorage;
import com.lodestar.datastore.storage.IStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/** Makes the inventory in front part of the network storage. Its filter limits what the network puts there. */
public class LinkBE extends PortBE {
    public LinkBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, false);
    }

    @Nullable
    public IStorage storage() {
        IItemHandler h = target();
        return h == null ? null : new HandlerStorage(h, priority, this::passes);
    }

    @Override
    public void targetChanged() {
        DataGrid g = currentGrid();
        if (g != null) g.markSourcesDirty();
    }

    @Override
    protected void filterChanged() {
        targetChanged();
    }

    @Override
    public boolean usesAugments() {
        return false;
    }

    @Override
    public boolean usesPriority() {
        return true;
    }

    @Override
    protected void nodeTick() {}

    @Override
    protected void work(DataGrid g, IItemHandler h) {}
}
