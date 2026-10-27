package com.lodestar.ducts;

import com.lodestar.core.grid.Grid;
import com.lodestar.core.grid.GridNodeBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A network of ducts of one kind. Collects the network's endpoints (sides that touch a non-duct block) when it is
 * built; energy and fluid networks share one buffer split back into the ducts when the network changes.
 */
public abstract class DuctGrid extends Grid {
    /** A duct side touching a block that holds this kind of stuff. */
    public record End(DuctBE duct, Direction face) {
        public BlockPos target() {
            return duct.getBlockPos().relative(face);
        }

        @Nullable
        public Attachment attachment() {
            return duct.attachment(face);
        }
    }

    protected final List<End> ends = new ArrayList<>();
    protected int rr;

    protected DuctGrid(Level level) {
        super(level);
    }

    protected List<DuctBE> ducts() {
        List<DuctBE> l = new ArrayList<>();
        for (GridNodeBE<?> n : nodeEntities) if (n instanceof DuctBE d) l.add(d);
        return l;
    }

    @Override
    protected void onBuilt() {
        ends.clear();
        for (DuctBE d : ducts()) {
            for (Direction f : Direction.values()) if (d.conn(f) == DuctBlock.Conn.END) ends.add(new End(d, f));
        }
    }

    /** Called from a duct's save while the network is live: write its current share without taking it out. */
    void shareFor(DuctBE be) {}

    void absorb(DuctBE be) {}

    void release(DuctBE be) {}

    @Nullable
    protected <T> T handler(End e, net.minecraftforge.common.capabilities.Capability<T> cap) {
        if (!level.isLoaded(e.target())) return null;
        BlockEntity be = level.getBlockEntity(e.target());
        if (be == null || be instanceof DuctBE) return null;
        return be.getCapability(cap, e.face().getOpposite()).resolve().orElse(null);
    }

    /** Ends in round-robin order starting one further each call. */
    protected List<End> rotated() {
        if (ends.size() < 2) return ends;
        List<End> l = new ArrayList<>(ends);
        Collections.rotate(l, -(rr++ % l.size()));
        return l;
    }

    // ================================================================== energy

    public static class Energy extends DuctGrid {
        public long buffer, capacity;

        public Energy(Level level) {
            super(level);
        }

        @Override
        protected void onBuilt() {
            super.onBuilt();
            capacity = 0;
            for (DuctBE d : ducts()) capacity += DuctTiers.idx(DuctTiers.ENERGY_BUFFER, d.tier);
            buffer = Math.min(buffer, capacity);
        }

        @Override
        void absorb(DuctBE be) {
            buffer += be.energyShare;
            be.energyShare = 0;
        }

        @Override
        void shareFor(DuctBE be) {
            be.energyShare = capacity == 0 ? 0 : buffer * DuctTiers.idx(DuctTiers.ENERGY_BUFFER, be.tier) / capacity;
        }

        @Override
        protected void onInvalidate() {
            long left = buffer;
            List<DuctBE> ds = ducts();
            for (DuctBE d : ds) {
                shareFor(d);
                left -= d.energyShare;
            }
            if (!ds.isEmpty()) ds.get(0).energyShare += left;
            buffer = 0;
        }

        public int receive(int amount, boolean sim) {
            int got = (int) Math.max(0, Math.min(amount, capacity - buffer));
            if (!sim) buffer += got;
            return got;
        }

        @Override
        protected void tick() {
            if (buffer <= 0 || ends.isEmpty()) return;
            List<End> l = rotated();
            List<IEnergyStorage> targets = new ArrayList<>();
            List<End> te = new ArrayList<>();
            for (End e : l) {
                IEnergyStorage s = handler(e, ForgeCapabilities.ENERGY);
                if (s != null && s.canReceive()) {
                    targets.add(s);
                    te.add(e);
                }
            }
            if (targets.isEmpty()) return;
            // even split first, then let anyone with room take what is left
            for (int pass = 0; pass < 2 && buffer > 0; pass++) {
                int n = targets.size();
                for (int i = 0; i < n && buffer > 0; i++) {
                    int rate = DuctTiers.idx(DuctTiers.ENERGY_RATE, te.get(i).duct().tier);
                    long share = pass == 0 ? Math.max(1, buffer / (n - i)) : buffer;
                    int offer = (int) Math.min(rate, share);
                    buffer -= targets.get(i).receiveEnergy(offer, false);
                }
            }
        }
    }

    // ================================================================== fluid

    public static class Fluids extends DuctGrid {
        public FluidStack buffer = FluidStack.EMPTY;
        public int capacity;

        public Fluids(Level level) {
            super(level);
        }

        @Override
        protected void onBuilt() {
            super.onBuilt();
            capacity = 0;
            for (DuctBE d : ducts()) capacity += DuctTiers.idx(DuctTiers.FLUID_BUFFER, d.tier);
            if (buffer.getAmount() > capacity) buffer.setAmount(capacity);
        }

        @Override
        void absorb(DuctBE be) {
            if (be.fluidShare.isEmpty()) return;
            if (buffer.isEmpty()) buffer = be.fluidShare.copy();
            else if (buffer.isFluidEqual(be.fluidShare)) buffer.grow(be.fluidShare.getAmount());
            // a different fluid meeting this one is lost, like mixing two liquids in one pipe
            be.fluidShare = FluidStack.EMPTY;
        }

        @Override
        void shareFor(DuctBE be) {
            if (buffer.isEmpty() || capacity == 0) {
                be.fluidShare = FluidStack.EMPTY;
                return;
            }
            int amt = (int) ((long) buffer.getAmount() * DuctTiers.idx(DuctTiers.FLUID_BUFFER, be.tier) / capacity);
            be.fluidShare = amt <= 0 ? FluidStack.EMPTY : new FluidStack(buffer, amt);
        }

        @Override
        protected void onInvalidate() {
            int left = buffer.getAmount();
            List<DuctBE> ds = ducts();
            for (DuctBE d : ds) {
                shareFor(d);
                left -= d.fluidShare.getAmount();
            }
            if (!ds.isEmpty() && left > 0 && !buffer.isEmpty()) {
                DuctBE first = ds.get(0);
                if (first.fluidShare.isEmpty()) first.fluidShare = new FluidStack(buffer, left);
                else first.fluidShare.grow(left);
            }
            buffer = FluidStack.EMPTY;
        }

        public int fill(FluidStack in, boolean execute) {
            if (in.isEmpty()) return 0;
            if (!buffer.isEmpty() && !buffer.isFluidEqual(in)) return 0;
            int room = capacity - buffer.getAmount();
            int amt = Math.min(room, in.getAmount());
            if (amt <= 0) return 0;
            if (execute) {
                if (buffer.isEmpty()) buffer = new FluidStack(in, amt);
                else buffer.grow(amt);
            }
            return amt;
        }

        private boolean isServo(End e) {
            Attachment a = e.attachment();
            return a != null && a.type == Attachment.Type.SERVO;
        }

        @Override
        protected void tick() {
            if (ends.isEmpty()) return;
            // servos pull
            for (End e : ends) {
                if (!isServo(e) || level.hasNeighborSignal(e.duct().getBlockPos())) continue;
                IFluidHandler h = handler(e, ForgeCapabilities.FLUID_HANDLER);
                if (h == null) continue;
                int rate = DuctTiers.idx(DuctTiers.FLUID_RATE, e.duct().tier);
                int room = Math.min(rate, capacity - buffer.getAmount());
                if (room <= 0) continue;
                FluidStack sim = buffer.isEmpty() ? h.drain(room, IFluidHandler.FluidAction.SIMULATE)
                        : h.drain(new FluidStack(buffer, room), IFluidHandler.FluidAction.SIMULATE);
                if (sim.isEmpty() || !e.attachment().allows(sim)) continue;
                int accepted = fill(sim, false);
                if (accepted <= 0) continue;
                FluidStack got = h.drain(new FluidStack(sim, accepted), IFluidHandler.FluidAction.EXECUTE);
                fill(got, true);
            }
            if (buffer.isEmpty()) return;
            // push to every other end that will take it
            List<End> l = rotated();
            List<IFluidHandler> targets = new ArrayList<>();
            List<End> te = new ArrayList<>();
            for (End e : l) {
                if (isServo(e)) continue;
                Attachment a = e.attachment();
                if (a != null && !a.allows(buffer)) continue;
                IFluidHandler h = handler(e, ForgeCapabilities.FLUID_HANDLER);
                if (h != null && h.fill(new FluidStack(buffer, 1), IFluidHandler.FluidAction.SIMULATE) > 0) {
                    targets.add(h);
                    te.add(e);
                }
            }
            int n = targets.size();
            for (int i = 0; i < n && !buffer.isEmpty(); i++) {
                int rate = DuctTiers.idx(DuctTiers.FLUID_RATE, te.get(i).duct().tier);
                int offer = Math.min(rate, Math.max(1, buffer.getAmount() / (n - i)));
                int done = targets.get(i).fill(new FluidStack(buffer, offer), IFluidHandler.FluidAction.EXECUTE);
                buffer.shrink(done);
            }
            if (buffer.getAmount() <= 0) buffer = FluidStack.EMPTY;
        }
    }

    // ================================================================== items

    public static class Items extends DuctGrid {
        /** links between ducts in this network, built once */
        private final Map<BlockPos, List<BlockPos>> links = new HashMap<>();
        private final Map<BlockPos, DuctBE> byPos = new HashMap<>();
        /** cached breadth-first trees from each duct that has needed one */
        private final Map<BlockPos, Map<BlockPos, BlockPos>> trees = new HashMap<>();
        private final Map<BlockPos, List<End>> nearest = new HashMap<>();

        public Items(Level level) {
            super(level);
        }

        @Override
        protected void onBuilt() {
            super.onBuilt();
            for (DuctBE d : ducts()) byPos.put(d.getBlockPos(), d);
            for (DuctBE d : ducts()) {
                List<BlockPos> l = new ArrayList<>();
                for (Direction f : Direction.values()) {
                    BlockPos np = d.getBlockPos().relative(f);
                    if (d.conn(f) == DuctBlock.Conn.DUCT && byPos.containsKey(np)) l.add(np);
                }
                links.put(d.getBlockPos(), l);
            }
        }

        @Override
        protected void tick() {}

        /** Breadth-first tree from a duct; ends sorted by distance. */
        private void search(BlockPos from) {
            if (trees.containsKey(from)) return;
            Map<BlockPos, BlockPos> parent = new HashMap<>();
            Map<BlockPos, Integer> dist = new HashMap<>();
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            q.add(from);
            parent.put(from, from);
            dist.put(from, 0);
            while (!q.isEmpty()) {
                BlockPos p = q.poll();
                for (BlockPos n : links.getOrDefault(p, List.of())) {
                    if (parent.containsKey(n)) continue;
                    parent.put(n, p);
                    dist.put(n, dist.get(p) + 1);
                    q.add(n);
                }
            }
            List<End> sorted = new ArrayList<>();
            for (End e : ends) if (dist.containsKey(e.duct().getBlockPos())) sorted.add(e);
            sorted.sort((a, b) -> Integer.compare(dist.get(a.duct().getBlockPos()), dist.get(b.duct().getBlockPos())));
            trees.put(from, parent);
            nearest.put(from, sorted);
        }

        /** Positions after {@code from} up to and including {@code to}. */
        private List<BlockPos> path(BlockPos from, BlockPos to) {
            search(from);
            Map<BlockPos, BlockPos> parent = trees.get(from);
            List<BlockPos> l = new ArrayList<>();
            BlockPos p = to;
            while (!p.equals(from)) {
                l.add(p);
                p = parent.get(p);
                if (p == null) return List.of();
            }
            Collections.reverse(l);
            return l;
        }

        private boolean receives(End e) {
            Attachment a = e.attachment();
            return a == null || a.type == Attachment.Type.FILTER;
        }

        /** How much of the stack the nearest accepting end will take, and that end. */
        @Nullable
        private End destination(BlockPos from, @Nullable End exclude, ItemStack stack) {
            search(from);
            for (End e : nearest.get(from)) {
                if (exclude != null && e.duct() == exclude.duct() && e.face() == exclude.face()) continue;
                if (!receives(e)) continue;
                Attachment a = e.attachment();
                if (a != null && !a.allows(stack)) continue;
                IItemHandler h = handler(e, ForgeCapabilities.ITEM_HANDLER);
                if (h == null) continue;
                ItemStack rem = ItemHandlerHelper.insertItemStacked(h, stack, true);
                if (rem.getCount() < stack.getCount()) return e;
            }
            return null;
        }

        private int accepted(End e, ItemStack stack) {
            IItemHandler h = handler(e, ForgeCapabilities.ITEM_HANDLER);
            if (h == null) return 0;
            return stack.getCount() - ItemHandlerHelper.insertItemStacked(h, stack, true).getCount();
        }

        private void send(DuctBE from, Direction srcFace, End dest, ItemStack stack) {
            List<BlockPos> p = new ArrayList<>(path(from.getBlockPos(), dest.duct().getBlockPos()));
            float speed = DuctTiers.idx(DuctTiers.ITEM_SPEED, from.tier);
            from.addItem(new TravellingItem(stack, p, from.getBlockPos(), srcFace, dest.face(), speed));
        }

        /** A servo on {@code face} of {@code duct} pulls one batch from the inventory it faces. */
        public void servoPull(DuctBE duct, Direction face, Attachment a) {
            End self = new End(duct, face);
            IItemHandler h = handler(self, ForgeCapabilities.ITEM_HANDLER);
            if (h == null) return;
            int max = DuctTiers.idx(DuctTiers.ITEM_STACK, duct.tier);
            int slots = h.getSlots();
            for (int k = 0; k < slots; k++) {
                int slot = (a.roundRobin + k) % slots;
                ItemStack sim = h.extractItem(slot, max, true);
                if (sim.isEmpty() || !a.allows(sim)) continue;
                End dest = destination(duct.getBlockPos(), self, sim);
                if (dest == null) continue;
                int n = accepted(dest, sim);
                ItemStack got = h.extractItem(slot, n, false);
                if (got.isEmpty()) continue;
                send(duct, face, dest, got);
                a.roundRobin = (slot + 1) % slots;
                return;
            }
        }

        /** A retriever fetches one batch of a matching item from any other inventory on the network. */
        public void retrieve(DuctBE duct, Direction face, Attachment a) {
            if (a.filterEmpty()) return;
            End self = new End(duct, face);
            IItemHandler into = handler(self, ForgeCapabilities.ITEM_HANDLER);
            if (into == null) return;
            int max = DuctTiers.idx(DuctTiers.ITEM_STACK, duct.tier);
            search(duct.getBlockPos());
            for (End src : nearest.get(duct.getBlockPos())) {
                if (src.duct() == duct && src.face() == face) continue;
                Attachment sa = src.attachment();
                if (sa != null && sa.type == Attachment.Type.RETRIEVER) continue;
                IItemHandler h = handler(src, ForgeCapabilities.ITEM_HANDLER);
                if (h == null) continue;
                for (int slot = 0; slot < h.getSlots(); slot++) {
                    ItemStack sim = h.extractItem(slot, max, true);
                    if (sim.isEmpty() || !a.allows(sim)) continue;
                    int n = sim.getCount() - ItemHandlerHelper.insertItemStacked(into, sim, true).getCount();
                    if (n <= 0) return;
                    ItemStack got = h.extractItem(slot, n, false);
                    if (got.isEmpty()) continue;
                    List<BlockPos> p = new ArrayList<>(path(src.duct().getBlockPos(), duct.getBlockPos()));
                    src.duct().addItem(new TravellingItem(got, p, src.duct().getBlockPos(), src.face(), face,
                            DuctTiers.idx(DuctTiers.ITEM_SPEED, src.duct().tier)));
                    return;
                }
            }
        }

        /** Something pushed a stack into the duct from {@code side}; route what can be delivered. */
        public ItemStack inject(DuctBE duct, Direction side, ItemStack stack, boolean sim) {
            End self = new End(duct, side);
            End dest = destination(duct.getBlockPos(), self, stack);
            if (dest == null) return stack;
            int n = accepted(dest, stack);
            if (n <= 0) return stack;
            if (!sim) send(duct, side, dest, ItemHandlerHelper.copyStackWithSize(stack, n));
            return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - n);
        }

        /** Delivery failed at {@code at}: send the rest elsewhere, or back where it came from. */
        public boolean reroute(DuctBE at, TravellingItem ti) {
            End dest = destination(at.getBlockPos(), null, ti.stack);
            if (dest == null) {
                DuctBE src = byPos.get(ti.source);
                if (src == null) return false;
                End back = new End(src, ti.sourceFace);
                if (accepted(back, ti.stack) <= 0) return false;
                dest = back;
            }
            ti.path.clear();
            ti.path.addAll(path(at.getBlockPos(), dest.duct().getBlockPos()));
            ti.destFace = dest.face();
            return true;
        }
    }
}
