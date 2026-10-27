package com.lodestar.core.grid;

import com.lodestar.core.block.LSBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Block entity that is a member of a {@link Grid}. Subclasses say which neighbours they link to
 * ({@link #linksTo}) and move their stored share of shared buffers in and out ({@link #absorbInto}/{@link #releaseFrom}).
 */
public abstract class GridNodeBE<G extends Grid> extends LSBlockEntity {
    @Nullable
    protected G grid;

    protected GridNodeBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** A fresh empty grid of this node's kind. */
    protected abstract G createGrid();

    /** Whether this node joins grids with that neighbouring node on side d (both sides must agree). */
    public abstract boolean linksTo(Direction d, GridNodeBE<?> other);

    /** Add this node's stored share to a newly built grid. */
    protected void absorbInto(G grid) {}

    /** Take this node's share back from a grid that is being invalidated. */
    protected void releaseFrom(G grid) {}

    @SuppressWarnings("unchecked")
    public G grid() {
        if (grid == null || !grid.isValid()) rebuild();
        return grid;
    }

    @Nullable
    public G currentGrid() {
        return grid;
    }

    void assign(Grid g) {
        @SuppressWarnings("unchecked") G gg = (G) g;
        this.grid = gg;
    }

    void release(Grid g) {
        @SuppressWarnings("unchecked") G gg = (G) g;
        releaseFrom(gg);
        if (this.grid == g) this.grid = null;
    }

    @SuppressWarnings("unchecked")
    private void rebuild() {
        if (level == null) return;
        G g = createGrid();
        Deque<GridNodeBE<?>> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(this);
        seen.add(worldPosition);
        while (!queue.isEmpty()) {
            GridNodeBE<?> n = queue.poll();
            if (n.grid != null && n.grid != g && n.grid.isValid()) n.grid.invalidate();
            n.assign(g);
            g.addNode(n);
            ((GridNodeBE<G>) n).absorbInto(g);
            for (Direction d : Direction.values()) {
                BlockPos np = n.worldPosition.relative(d);
                if (seen.contains(np) || !level.isLoaded(np)) continue;
                BlockEntity be = level.getBlockEntity(np);
                if (be instanceof GridNodeBE<?> other && !other.isRemoved() && n.linksTo(d, other) && other.linksTo(d.getOpposite(), n)) {
                    seen.add(np);
                    queue.add(other);
                }
            }
        }
        this.grid = g;
        g.onBuilt();
    }

    /** Forces the grid to rebuild (connections changed). */
    public void invalidateGrid() {
        if (grid != null) grid.invalidate();
    }

    @Override
    public void tickServer() {
        G g = grid();
        if (g != null) g.tickOnce();
        nodeTick();
    }

    /** Per-node work after the grid tick. */
    protected void nodeTick() {}

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            for (Direction d : Direction.values()) {
                BlockPos np = worldPosition.relative(d);
                if (!level.isLoaded(np)) continue;
                if (level.getBlockEntity(np) instanceof GridNodeBE<?> other) other.invalidateGrid();
            }
        }
    }

    @Override
    public void setRemoved() {
        if (grid != null) grid.invalidate();
        super.setRemoved();
    }
}
