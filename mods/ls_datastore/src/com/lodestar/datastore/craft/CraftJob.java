package com.lodestar.datastore.craft;

import com.lodestar.datastore.DataGrid;
import com.lodestar.datastore.FabricatorBE;
import com.lodestar.datastore.ItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A running crafting request. Takes the planned items out of storage into its own buffer, hands one craft at a time to
 * idle fabricators that hold the right card, collects what they make, and stores the results when every step is done.
 */
public final class CraftJob {
    public static final int GATHERING = 0, CRAFTING = 1, NO_FABRICATOR = 2, STORING = 3;

    static final class Step {
        final ItemStack card;
        final ItemKey cardKey;
        int remaining;
        @Nullable
        Pattern pattern;

        Step(ItemStack card, int remaining) {
            this.card = card.copyWithCount(1);
            this.cardKey = ItemKey.of(this.card);
            this.remaining = remaining;
        }

        @Nullable
        Pattern pattern(Level level) {
            if (pattern == null) pattern = Pattern.of(card, level);
            return pattern;
        }
    }

    public final int id;
    public final ItemKey target;
    public final long count;
    private final List<Step> steps = new ArrayList<>();
    private final Map<ItemKey, Long> buffer = new LinkedHashMap<>();
    private final Map<ItemKey, Long> toGather = new LinkedHashMap<>();
    private long done, total;
    private int state = GATHERING;

    private CraftJob(int id, ItemKey target, long count) {
        this.id = id;
        this.target = target;
        this.count = count;
    }

    public static CraftJob of(int id, CraftPlanner.Plan plan) {
        CraftJob j = new CraftJob(id, plan.target, plan.count);
        for (CraftPlanner.Step s : plan.steps) {
            Step st = new Step(s.pattern().card, s.crafts());
            st.pattern = s.pattern();
            j.steps.add(st);
            j.total += s.crafts();
        }
        j.toGather.putAll(plan.used);
        return j;
    }

    public int state() {
        return state;
    }

    public long done() {
        return done;
    }

    public long total() {
        return total;
    }

    /** A fabricator finished a craft for this job. */
    public void receive(List<ItemStack> stacks) {
        for (ItemStack s : stacks) if (!s.isEmpty()) buffer.merge(ItemKey.of(s), (long) s.getCount(), Long::sum);
    }

    /** One tick of work; returns true when the job is finished and can be dropped. */
    public boolean tick(DataGrid g, Level level) {
        if (!toGather.isEmpty() && level.getGameTime() % 10 == 0) {
            Iterator<Map.Entry<ItemKey, Long>> it = toGather.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ItemKey, Long> e = it.next();
                long got = g.extract(e.getKey(), e.getValue(), false);
                if (got > 0) buffer.merge(e.getKey(), got, Long::sum);
                if (got >= e.getValue()) it.remove();
                else e.setValue(e.getValue() - got);
            }
        }
        int busy = 0;
        for (FabricatorBE f : g.fabricators) if (!f.isRemoved() && f.workingFor(id)) busy++;
        boolean remaining = false, noFab = false;
        for (Step s : steps) {
            if (s.remaining <= 0) continue;
            remaining = true;
            Pattern p = s.pattern(level);
            if (p == null) {
                noFab = true;
                continue;
            }
            boolean anyHolder = false;
            for (FabricatorBE f : g.fabricators) {
                if (s.remaining <= 0) break;
                if (f.isRemoved() || !f.holds(s.cardKey)) continue;
                anyHolder = true;
                if (!f.idle()) continue;
                ItemStack[] items = p.take(buffer);
                if (items == null) break;
                if (f.start(id, p, items)) {
                    s.remaining--;
                    done++;
                    busy++;
                } else {
                    for (ItemStack it : items) if (!it.isEmpty()) buffer.merge(ItemKey.of(it), 1L, Long::sum);
                    break;
                }
            }
            if (!anyHolder) noFab = true;
        }
        if (remaining) {
            state = !toGather.isEmpty() && busy == 0 ? GATHERING : noFab && busy == 0 ? NO_FABRICATOR : CRAFTING;
            return false;
        }
        if (busy > 0) {
            state = CRAFTING;
            return false;
        }
        state = STORING;
        flush(g);
        return buffer.isEmpty() && toGather.isEmpty();
    }

    /** Puts the buffer back into the network (results, leftovers, or everything on cancel). */
    public void flush(DataGrid g) {
        Iterator<Map.Entry<ItemKey, Long>> it = buffer.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ItemKey, Long> e = it.next();
            long stored = g.insert(e.getKey(), e.getValue(), false);
            if (stored >= e.getValue()) it.remove();
            else e.setValue(e.getValue() - stored);
        }
    }

    /** Cancels: nothing more is crafted or gathered. */
    public void cancel() {
        for (Step s : steps) s.remaining = 0;
        toGather.clear();
    }

    /** What is held in the buffer, as stacks (for dropping when the core is broken). */
    public List<ItemStack> bufferStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> e : buffer.entrySet()) {
            long n = e.getValue();
            while (n > 0) {
                int c = (int) Math.min(n, e.getKey().maxStack());
                out.add(e.getKey().stack(c));
                n -= c;
            }
        }
        return out;
    }

    public CompoundTag summary() {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.put("T", target.save());
        t.putLong("N", count);
        t.putLong("Done", done);
        t.putLong("Total", total);
        t.putInt("State", state);
        return t;
    }

    // ------------------------------------------------------------------ persistence

    private static ListTag saveMap(Map<ItemKey, Long> m) {
        ListTag l = new ListTag();
        for (Map.Entry<ItemKey, Long> e : m.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.put("K", e.getKey().save());
            c.putLong("N", e.getValue());
            l.add(c);
        }
        return l;
    }

    private static void loadMap(ListTag l, Map<ItemKey, Long> into) {
        for (Tag t : l) {
            CompoundTag c = (CompoundTag) t;
            ItemKey k = ItemKey.load(c.getCompound("K"));
            if (k != null && c.getLong("N") > 0) into.merge(k, c.getLong("N"), Long::sum);
        }
    }

    public CompoundTag save() {
        CompoundTag t = summary();
        ListTag st = new ListTag();
        for (Step s : steps) {
            CompoundTag c = new CompoundTag();
            c.put("Card", s.card.save(new CompoundTag()));
            c.putInt("Left", s.remaining);
            st.add(c);
        }
        t.put("Steps", st);
        t.put("Buffer", saveMap(buffer));
        t.put("Gather", saveMap(toGather));
        return t;
    }

    @Nullable
    public static CraftJob load(CompoundTag t) {
        ItemKey target = ItemKey.load(t.getCompound("T"));
        if (target == null) return null;
        CraftJob j = new CraftJob(t.getInt("Id"), target, t.getLong("N"));
        j.done = t.getLong("Done");
        j.total = t.getLong("Total");
        j.state = t.getInt("State");
        for (Tag s : t.getList("Steps", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) s;
            ItemStack card = ItemStack.of(c.getCompound("Card"));
            if (!card.isEmpty()) j.steps.add(new Step(card, c.getInt("Left")));
        }
        loadMap(t.getList("Buffer", Tag.TAG_COMPOUND), j.buffer);
        loadMap(t.getList("Gather", Tag.TAG_COMPOUND), j.toGather);
        return j;
    }
}
