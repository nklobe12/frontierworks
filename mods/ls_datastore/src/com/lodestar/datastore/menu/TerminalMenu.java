package com.lodestar.datastore.menu;

import com.lodestar.core.net.CoreNet;
import com.lodestar.datastore.CoreBE;
import com.lodestar.datastore.DataGrid;
import com.lodestar.datastore.DatastoreSetup;
import com.lodestar.datastore.ItemKey;
import com.lodestar.datastore.RemoteTerminalItem;
import com.lodestar.datastore.TerminalBE;
import com.lodestar.datastore.craft.CraftJob;
import com.lodestar.datastore.craft.CraftPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Access / crafting terminal (and remote terminal). Network items are not slots: the server sends the client a delta of
 * the network contents every half second and the client sends click actions back ({@link #handle}). The crafting variant
 * adds a 3x3 grid kept in the terminal block that refills from the network as you craft.
 */
public class TerminalMenu extends AbstractContainerMenu {
    public static final String SYNC = "ls_datastore:items", ACTION = "ls_datastore:term", PLAN = "ls_datastore:plan", JOBS = "ls_datastore:jobs";
    public static final int A_TAKE = 0, A_TAKE_SHIFT = 1, A_INSERT = 2, A_PLAN = 3, A_START = 4, A_CLEAR = 5, A_JOBS = 6, A_CANCEL = 7;
    public static final int OUT_OF_RANGE = 4;
    private static final int BATCH = 400;

    public final boolean crafting, remote;
    private final Player player;
    @Nullable
    private final Supplier<DataGrid> net;
    private final Predicate<Player> valid;
    @Nullable
    private final TerminalBE be;
    @Nullable
    public final CraftingContainer craftGrid;
    @Nullable
    private final ResultContainer result;
    private final int invStart;

    private final Map<ItemKey, Long> sent = new HashMap<>();
    private Set<ItemKey> sentCraft = new HashSet<>();
    private int sentStatus = -1;
    private int timer;
    private boolean force = true, first = true;

    public static void writeOpen(FriendlyByteBuf buf, boolean crafting, boolean remote) {
        buf.writeBoolean(crafting);
        buf.writeBoolean(remote);
    }

    public static int rows(boolean crafting) {
        return crafting ? 4 : 6;
    }

    public static int gridY(boolean crafting) {
        return 18 + rows(crafting) * 18 + 6;
    }

    public static int invY(boolean crafting) {
        return crafting ? gridY(true) + 54 + 12 : 18 + rows(false) * 18 + 14;
    }

    /** Client side. */
    public TerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBoolean(), buf.readBoolean(), null, p -> true, null);
    }

    /** Server side, from a terminal block. */
    public TerminalMenu(int id, Inventory inv, TerminalBE be) {
        this(id, inv, be.crafting, false, () -> be.isRemoved() ? null : be.grid(),
                p -> !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5) < 64, be);
    }

    /** Server side, from a remote terminal linked to a core. */
    public static TerminalMenu remote(int id, Inventory inv, BlockPos core) {
        Level level = inv.player.level();
        Supplier<DataGrid> net = () -> level.isLoaded(core) && level.getBlockEntity(core) instanceof CoreBE c ? c.grid() : null;
        Predicate<Player> valid = p -> p.distanceToSqr(core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5) <= RemoteTerminalItem.RANGE * RemoteTerminalItem.RANGE
                && level.isLoaded(core) && level.getBlockEntity(core) instanceof CoreBE;
        return new TerminalMenu(id, inv, false, true, net, valid, null);
    }

    private TerminalMenu(int id, Inventory inv, boolean crafting, boolean remote, @Nullable Supplier<DataGrid> net, Predicate<Player> valid, @Nullable TerminalBE be) {
        super(DatastoreSetup.TERMINAL_MENU.get(), id);
        this.crafting = crafting;
        this.remote = remote;
        this.player = inv.player;
        this.net = net;
        this.valid = valid;
        this.be = be;
        if (crafting) {
            craftGrid = be != null ? new TransientCraftingContainer(this, 3, 3, be.matrix) : new TransientCraftingContainer(this, 3, 3);
            result = new ResultContainer();
            int gy = gridY(true);
            for (int i = 0; i < 9; i++) addSlot(new Slot(craftGrid, i, 26 + (i % 3) * 18, gy + (i / 3) * 18));
            addSlot(new RefillSlot(inv.player, craftGrid, result, 120, gy + 18));
        } else {
            craftGrid = null;
            result = null;
        }
        invStart = slots.size();
        int iy = invY(crafting);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, iy + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, iy + 58));
        if (crafting && be != null) slotsChanged(craftGrid);
    }

    @Nullable
    private DataGrid network() {
        if (net == null) return null;
        DataGrid g = net.get();
        return g != null && g.online() ? g : null;
    }

    // ------------------------------------------------------------------ crafting grid

    @Override
    public void slotsChanged(@Nonnull Container c) {
        super.slotsChanged(c);
        if (c != craftGrid || result == null || player.level().isClientSide) return;
        Level level = player.level();
        Optional<CraftingRecipe> r = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftGrid, level);
        ItemStack out = ItemStack.EMPTY;
        if (r.isPresent() && result.setRecipeUsed(level, (ServerPlayer) player, r.get())) {
            ItemStack a = r.get().assemble(craftGrid, level.registryAccess());
            if (a.isItemEnabled(level.enabledFeatures())) out = a;
        }
        result.setItem(0, out);
        if (be != null) be.setChanged();
    }

    /** Result slot that takes replacements for used-up grid items from the network. */
    private class RefillSlot extends ResultSlot {
        private final CraftingContainer grid;

        RefillSlot(Player p, CraftingContainer grid, Container result, int x, int y) {
            super(p, grid, result, 0, x, y);
            this.grid = grid;
        }

        @Override
        public void onTake(@Nonnull Player p, @Nonnull ItemStack stack) {
            ItemStack[] before = new ItemStack[9];
            for (int i = 0; i < 9; i++) before[i] = grid.getItem(i).copy();
            super.onTake(p, stack);
            if (p.level().isClientSide) return;
            DataGrid g = network();
            if (g == null) return;
            for (int i = 0; i < 9; i++) {
                if (before[i].isEmpty() || !grid.getItem(i).isEmpty()) continue;
                ItemStack got = g.extractStack(ItemKey.of(before[i]), 1, false);
                if (!got.isEmpty()) grid.setItem(i, got);
            }
        }
    }

    // ------------------------------------------------------------------ sync

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || net == null) return;
        if (!force && ++timer < 10) return;
        timer = 0;
        force = false;
        DataGrid g = net.get();
        int status = g == null ? DataGrid.NO_CORE : g.status();
        Map<ItemKey, Long> now = g != null && g.online() ? g.snapshot() : Collections.emptyMap();
        Set<ItemKey> craft = g != null ? g.craftable() : Collections.emptySet();
        ListTag changes = new ListTag();
        for (Map.Entry<ItemKey, Long> e : now.entrySet()) {
            Long old = sent.get(e.getKey());
            boolean c = craft.contains(e.getKey());
            if (old == null || !old.equals(e.getValue()) || sentCraft.contains(e.getKey()) != c) changes.add(entry(e.getKey(), e.getValue(), c));
        }
        for (ItemKey k : craft) {
            if (now.containsKey(k)) continue;
            Long old = sent.get(k);
            if (old == null || old != 0 || !sentCraft.contains(k)) changes.add(entry(k, 0, true));
        }
        for (ItemKey k : sent.keySet()) if (!now.containsKey(k) && !craft.contains(k)) changes.add(entry(k, -1, false));
        sent.clear();
        sent.putAll(now);
        for (ItemKey k : craft) sent.putIfAbsent(k, 0L);
        sentCraft = new HashSet<>(craft);
        if (changes.isEmpty() && status == sentStatus && !first) return;
        sentStatus = status;
        int n = Math.max(1, (changes.size() + BATCH - 1) / BATCH);
        for (int b = 0; b < n; b++) {
            CompoundTag t = new CompoundTag();
            t.putInt("W", containerId);
            t.putBoolean("Full", first && b == 0);
            t.putInt("St", status);
            ListTag part = new ListTag();
            for (int i = b * BATCH; i < Math.min(changes.size(), (b + 1) * BATCH); i++) part.add(changes.get(i));
            t.put("E", part);
            CoreNet.toPlayer(sp, SYNC, t);
        }
        first = false;
    }

    private static CompoundTag entry(ItemKey k, long n, boolean craftable) {
        CompoundTag c = new CompoundTag();
        c.put("K", k.save());
        c.putLong("N", n);
        if (craftable) c.putBoolean("C", true);
        return c;
    }

    // ------------------------------------------------------------------ actions from the client

    public void handle(ServerPlayer sp, CompoundTag t) {
        int a = t.getInt("A");
        ItemKey k = t.contains("K") ? ItemKey.load(t.getCompound("K")) : null;
        DataGrid g = network();
        force = true;
        switch (a) {
            case A_TAKE -> {
                if (g == null || k == null || !getCarried().isEmpty()) return;
                long have = g.count(k);
                int n = (int) Math.min(have, k.maxStack());
                if (t.getInt("B") == 1) n = (n + 1) / 2;
                ItemStack got = g.extractStack(k, n, false);
                setCarried(got);
            }
            case A_TAKE_SHIFT -> {
                if (g == null || k == null) return;
                ItemStack got = g.extractStack(k, k.maxStack(), false);
                if (got.isEmpty()) return;
                moveItemStackTo(got, invStart, slots.size(), true);
                if (!got.isEmpty()) g.insert(got, false);
            }
            case A_INSERT -> {
                ItemStack c = getCarried();
                if (g == null || c.isEmpty()) return;
                if (t.getInt("B") == 1) {
                    if (g.insert(c.copyWithCount(1), false).isEmpty()) c.shrink(1);
                } else {
                    setCarried(g.insert(c, false));
                }
            }
            case A_PLAN -> {
                if (g == null || k == null) return;
                CraftPlanner.Plan p = CraftPlanner.plan(g.snapshot(), g.patterns(), k, clampCount(t.getLong("N")));
                CompoundTag out = p.toTag();
                out.putInt("W", containerId);
                CoreNet.toPlayer(sp, PLAN, out);
            }
            case A_START -> {
                if (g == null || k == null || g.core == null) return;
                CraftPlanner.Plan p = CraftPlanner.plan(g.snapshot(), g.patterns(), k, clampCount(t.getLong("N")));
                if (!p.ok()) return;
                g.core.start(p);
                sp.displayClientMessage(Component.translatable("msg.ls_datastore.job_started", p.count, k.proto().getHoverName()), true);
            }
            case A_CLEAR -> {
                if (craftGrid == null) return;
                for (int i = 0; i < 9; i++) {
                    ItemStack s = craftGrid.getItem(i);
                    if (s.isEmpty()) continue;
                    ItemStack rem = g == null ? s : g.insert(s, false);
                    if (!rem.isEmpty() && !sp.getInventory().add(rem)) sp.drop(rem, false);
                    craftGrid.setItem(i, ItemStack.EMPTY);
                }
            }
            case A_JOBS -> sendJobs(sp);
            case A_CANCEL -> {
                DataGrid any = net == null ? null : net.get();
                if (any != null && any.core != null && any.online()) any.core.cancel(t.getInt("J"), any);
                sendJobs(sp);
            }
            default -> {}
        }
    }

    private static long clampCount(long n) {
        return Math.max(1, Math.min(n, 100_000));
    }

    private void sendJobs(ServerPlayer sp) {
        DataGrid g = net == null ? null : net.get();
        CompoundTag out = new CompoundTag();
        out.putInt("W", containerId);
        ListTag l = new ListTag();
        if (g != null && g.core != null) for (CraftJob j : g.core.jobs) l.add(j.summary());
        out.put("Jobs", l);
        CoreNet.toPlayer(sp, JOBS, out);
    }

    // ------------------------------------------------------------------ slots

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (crafting && index == 9) {
            // crafting result: craft into the inventory, refilling the grid as we go
            stack.getItem().onCraftedBy(stack, p.level(), p);
            if (!moveItemStackTo(stack, invStart, slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, stack);
            p.drop(stack, false);
            return copy;
        }
        if (p.level().isClientSide) return ItemStack.EMPTY;
        DataGrid g = network();
        if (index < invStart) {
            // crafting grid back to the network, else the inventory
            ItemStack rem = g == null ? stack : g.insert(stack, false);
            if (!rem.isEmpty() && !moveItemStackTo(rem, invStart, slots.size(), true)) {
                slot.set(rem);
                return ItemStack.EMPTY;
            }
            slot.set(rem);
            force = true;
            return ItemStack.EMPTY;
        }
        if (g == null) return ItemStack.EMPTY;
        slot.set(g.insert(stack, false));
        force = true;
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull Slot slot) {
        return result == null || slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(@Nonnull Player p) {
        return valid.test(p);
    }

    @Override
    public void removed(@Nonnull Player p) {
        super.removed(p);
        if (be != null) be.setChanged();
    }
}
