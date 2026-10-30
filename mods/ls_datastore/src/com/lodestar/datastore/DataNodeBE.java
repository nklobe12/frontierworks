package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.grid.GridNodeBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Any block that is part of a datastore network: cables and every device. */
public abstract class DataNodeBE extends GridNodeBE<DataGrid> {
    protected DataNodeBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected DataGrid createGrid() {
        return new DataGrid(level);
    }

    @Override
    public boolean linksTo(Direction d, GridNodeBE<?> other) {
        return other instanceof DataNodeBE && connectsOn(d);
    }

    /** Whether this node joins the network on side d (ports do not join through the face touching their inventory). */
    public boolean connectsOn(Direction d) {
        return true;
    }

    /** Energy this device costs the core every tick. */
    public int upkeep() {
        return 1;
    }

    /** The network if it is online, else null. Server side only. */
    @Nullable
    public DataGrid network() {
        if (level == null || level.isClientSide || isRemoved()) return null;
        DataGrid g = grid();
        return g != null && g.online() ? g : null;
    }

    @Override
    protected void nodeTick() {
        if (level.getGameTime() % 10 == (worldPosition.asLong() & 7)) updateLit(litNow());
    }

    /** What the LIT block state should show; devices light up while their network is online. */
    protected boolean litNow() {
        DataGrid g = grid();
        return g != null && g.online();
    }

    protected void updateLit(boolean on) {
        BlockState s = getBlockState();
        // clients only: a light change must not ripple neighbour updates through the cables
        if (s.hasProperty(LSEntityBlock.LIT) && s.getValue(LSEntityBlock.LIT) != on) level.setBlock(worldPosition, s.setValue(LSEntityBlock.LIT, on), 2);
    }
}
