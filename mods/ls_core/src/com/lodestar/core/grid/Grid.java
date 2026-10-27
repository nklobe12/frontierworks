package com.lodestar.core.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A connected network of {@link GridNodeBE}s (ducts, cables, pipes, storage buses). Built lazily by a breadth-first
 * search when a node asks for its grid; any change to membership invalidates it and it is rebuilt on next use.
 * Ticks once per game tick, driven by whichever member ticks first. Shared buffers (energy, fluid) are split back
 * into the nodes on invalidation so nothing is lost when grids merge, split or unload.
 */
public abstract class Grid {
    public final Level level;
    public final Set<BlockPos> nodes = new LinkedHashSet<>();
    protected final List<GridNodeBE<?>> nodeEntities = new ArrayList<>();
    private boolean valid = true;
    private long lastTick = Long.MIN_VALUE;

    protected Grid(Level level) {
        this.level = level;
    }

    public boolean isValid() {
        return valid;
    }

    public final void invalidate() {
        if (!valid) return;
        valid = false;
        onInvalidate();
        for (GridNodeBE<?> n : nodeEntities) n.release(this);
    }

    public final void tickOnce() {
        long t = level.getGameTime();
        if (t == lastTick || !valid) return;
        lastTick = t;
        tick();
    }

    void addNode(GridNodeBE<?> be) {
        nodes.add(be.getBlockPos());
        nodeEntities.add(be);
    }

    public List<GridNodeBE<?>> nodeEntities() {
        return nodeEntities;
    }

    public int size() {
        return nodes.size();
    }

    protected abstract void tick();

    /** Called once after the search has added every node (and each node absorbed its stored share). */
    protected void onBuilt() {}

    /** Called once when the grid stops being valid; give shared buffers back to the nodes here. */
    protected void onInvalidate() {}
}
