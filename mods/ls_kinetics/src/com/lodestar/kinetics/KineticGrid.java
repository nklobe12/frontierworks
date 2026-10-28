package com.lodestar.kinetics;

import com.lodestar.core.grid.Grid;
import com.lodestar.core.grid.GridNodeBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * A rotation network. When built it walks every coupling (shaft to shaft, gear to gear, belt to belt) from one root
 * and gives every node its speed ratio to the root; a gear loop that disagrees with itself jams the network. Every tick
 * it collects the sources, takes the fastest as the base speed (sources pulling opposite ways jam it), adds up the
 * torque the sources can give and the machines at its shaft ends need, both referred to the base speed (power is
 * conserved through gearing: a part geared up twice as fast loads the network with twice its torque), and stops when
 * the machines need more than the sources give.
 */
public class KineticGrid extends Grid {
    public static final float MAX_RPM = 256;

    public record End(KineticBE node, Direction face) {
        public BlockPos pos() {
            return node.getBlockPos().relative(face);
        }
    }

    private final List<KineticBE> members = new ArrayList<>();
    private final List<KineticBE> sources = new ArrayList<>();
    private final List<End> ends = new ArrayList<>();
    private final Set<BlockPos> endPositions = new HashSet<>();
    private boolean conflict;
    private boolean fresh = true;
    private double baseAngle;
    private double maxRatio = 1;
    public float baseSpeed, capacity, demand;
    public byte status;

    public KineticGrid(Level level) {
        super(level);
    }

    public List<KineticBE> members() {
        return Collections.unmodifiableList(members);
    }

    public List<End> ends() {
        return Collections.unmodifiableList(ends);
    }

    public boolean hasEndAt(BlockPos p) {
        return endPositions.contains(p);
    }

    @Override
    protected void onBuilt() {
        for (GridNodeBE<?> n : nodeEntities()) if (n instanceof KineticBE k) members.add(k);
        if (members.isEmpty()) return;
        // speed ratios by breadth-first search over couplings
        IdentityHashMap<KineticBE, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<KineticBE> queue = new ArrayDeque<>();
        KineticBE root = members.get(0);
        root.ratio = 1;
        seen.put(root, true);
        queue.add(root);
        while (!queue.isEmpty()) {
            KineticBE n = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos np = n.getBlockPos().relative(d);
                if (!level.isLoaded(np)) continue;
                if (!(level.getBlockEntity(np) instanceof KineticBE k) || k.currentGrid() != this) continue;
                double f = n.coupling(d, k);
                if (f == 0 || k.coupling(d.getOpposite(), n) == 0) continue;
                link(n, k, f, seen, queue);
            }
            for (BlockPos p : n.remoteLinks()) {
                if (!level.isLoaded(p)) continue;
                if (!(level.getBlockEntity(p) instanceof KineticBE k) || k.currentGrid() != this) continue;
                if (!k.remoteLinks().contains(n.getBlockPos())) continue;
                link(n, k, n.remoteFactor(k), seen, queue);
            }
        }
        maxRatio = 1e-9;
        for (KineticBE k : members) {
            maxRatio = Math.max(maxRatio, Math.abs(k.ratio));
            if (k instanceof IKineticSource) sources.add(k);
            for (Direction d : Direction.values()) {
                double ff = k.faceFactor(d);
                if (ff == 0 || !k.drivesFace(d)) continue;
                BlockPos p = k.getBlockPos().relative(d);
                if (!level.isLoaded(p)) continue;
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof IKineticConsumer c && !(be instanceof KineticBE) && c.acceptsRotation(d.getOpposite())) {
                    ends.add(new End(k, d));
                    endPositions.add(p);
                    maxRatio = Math.max(maxRatio, Math.abs(k.ratio * ff));
                }
            }
        }
        baseAngle = root.angleNow(level.getGameTime());
        fresh = true;
    }

    private void link(KineticBE n, KineticBE k, double f, IdentityHashMap<KineticBE, Boolean> seen, ArrayDeque<KineticBE> queue) {
        double r = n.ratio * f;
        if (seen.containsKey(k)) {
            if (Math.abs(k.ratio - r) > 1e-6 * Math.max(1, Math.abs(r))) conflict = true;
            return;
        }
        k.ratio = r;
        seen.put(k, true);
        queue.add(k);
    }

    @Nullable
    private IKineticConsumer consumer(End e) {
        BlockPos p = e.pos();
        if (!level.isLoaded(p)) return null;
        BlockEntity be = level.getBlockEntity(p);
        return be instanceof IKineticConsumer c && !be.isRemoved() ? c : null;
    }

    @Override
    protected void tick() {
        long now = level.getGameTime();
        float best = 0;
        int dir = 0;
        boolean clash = false;
        float cap = 0;
        for (KineticBE s : sources) {
            IKineticSource src = (IKineticSource) s;
            float sp = src.sourceSpeed();
            float tq = src.sourceTorque();
            if (sp == 0 || tq <= 0 || s.ratio == 0) continue;
            double b = sp / s.ratio;
            int sg = b > 0 ? 1 : -1;
            if (dir == 0) dir = sg;
            else if (sg != dir) clash = true;
            if (Math.abs(b) > Math.abs(best)) best = (float) b;
            cap += tq * Math.abs(s.ratio);
        }
        float dem = 0;
        for (End e : ends) {
            IKineticConsumer c = consumer(e);
            if (c != null) dem += c.torqueDemand() * Math.abs(e.node().ratio * e.node().faceFactor(e.face()));
        }
        byte st = KineticBE.OK;
        float base = 0;
        if (conflict || clash) st = KineticBE.JAMMED;
        else if (best != 0 && dem > cap + 1e-3f) st = KineticBE.OVERLOADED;
        else base = best;
        if (base != 0 && Math.abs(base) * maxRatio > MAX_RPM) base = (float) (Math.signum(base) * MAX_RPM / maxRatio);
        baseSpeed = base;
        status = st;
        capacity = cap;
        demand = dem;
        float util = cap > 0 ? Math.min(1f, dem / cap) : 0;
        for (KineticBE n : members) {
            if (n.isRemoved()) continue;
            n.applyNetwork((float) (base * n.ratio), st, util, baseAngle, now, fresh);
        }
        fresh = false;
        for (End e : ends) {
            IKineticConsumer c = consumer(e);
            if (c == null) continue;
            double rpm = base * e.node().ratio * e.node().faceFactor(e.face()) * e.face().getAxisDirection().getStep();
            c.setRotation(e.face().getOpposite(), (float) rpm, st);
        }
        for (KineticBE s : sources) ((IKineticSource) s).networkLoad(util, base != 0);
        baseAngle = (baseAngle + base * KineticBE.DEG_PER_TICK) % (360.0 * 4096);
    }
}
