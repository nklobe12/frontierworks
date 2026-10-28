package com.lodestar.quarry;

import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Shared machinery of the frame quarry and the laser quarry: the layer-by-layer dig of a rectangular area down to the
 * floor, drill bits (fortune / silk touch loot), sieves, an output buffer with overflow handling, and land restoration,
 * which keeps rubble and soil as spoil and fills the hole back in when the dig is over.
 */
public abstract class AbstractQuarryBE extends MachineBlockEntity {
    public enum Phase { NO_AREA, BUILDING, MINING, RESTORING, DONE, BLOCKED }

    public static final int S_BIT = 0, S_SIEVE = 1, S_FILL = 2, FILLS = 3, S_OUT = 5, OUTS = 9;
    public static final int BTN_RESTORE = 0, BTN_SHOW = 1, BTN_OVERFLOW = 2, BTN_RESET = 3, BTN_MINUS = 4, BTN_PLUS = 5;
    public static final int ST_OK = 0, ST_NO_POWER = 1, ST_FULL = 2, ST_NEEDS_FILL = 3, ST_NO_TOWER = 4;
    public static final int X_PHASE = 0, X_Y = 1, X_PROGRESS = 2, X_MINED = 3, X_COST = 4, X_FLAGS = 5, X_SIZE_A = 6, X_SIZE_B = 7,
            X_RUBBLE = 8, X_SOIL = 9, X_TOP = 10, X_FLOOR = 11, X_STATUS = 12, X_MASTS = 13, EXTRAS = 14;

    public static final TagKey<Block> IMMUNE = TagKey.create(Registries.BLOCK, new ResourceLocation("ls_quarry", "immune"));
    public static final TagKey<Item> FILL = TagKey.create(Registries.ITEM, new ResourceLocation("ls_quarry", "fill_material"));

    /** Layout shared by both quarries; the laser quarry adds radius buttons. */
    protected static MachineLayout layout(boolean laser) {
        MachineLayout.Builder b = MachineLayout.builder().energy(8, 17, 52)
                .slot(26, 17, MachineLayout.Role.FILTER).slot(26, 35, MachineLayout.Role.FILTER)
                .slot(48, 17, MachineLayout.Role.INPUT).slot(48, 35, MachineLayout.Role.INPUT).slot(48, 53, MachineLayout.Role.INPUT)
                .grid(80, 17, 3, 3, MachineLayout.Role.OUTPUT)
                .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
                .info(8, 73).extra(EXTRAS).height(216)
                .button(BTN_RESTORE, 8, 94, 78, 12, "gui.ls_quarry.restore.off")
                .button(BTN_SHOW, 90, 94, 78, 12, "gui.ls_quarry.show.off")
                .button(BTN_OVERFLOW, 8, 108, 78, 12, "gui.ls_quarry.overflow.hold");
        if (laser) {
            b.button(BTN_MINUS, 90, 108, 16, 12, "gui.ls_quarry.minus").button(BTN_PLUS, 108, 108, 16, 12, "gui.ls_quarry.plus")
                    .button(BTN_RESET, 128, 108, 40, 12, "gui.ls_quarry.reset");
        } else {
            b.button(BTN_RESET, 90, 108, 78, 12, "gui.ls_quarry.reset");
        }
        return b.build();
    }

    protected Phase phase = Phase.NO_AREA;
    protected boolean hasArea;
    /** dig area, inclusive interior bounds */
    protected int x0, z0, x1, z1, topY, floorY;
    protected int curY, index;
    protected int mined, lastCost, status;
    protected boolean restore, show, dropOverflow;
    protected int spoilRubble, spoilSoil;
    /** per column: highest solid block dug (Integer.MIN_VALUE: none) and what it was (1 soil, 2 sand, 3 other) */
    protected int[] surface = new int[0];
    protected byte[] topKind = new byte[0];
    protected int bottomY = Integer.MAX_VALUE;
    protected final List<ItemStack> pending = new ArrayList<>();
    protected double work;
    /** optional floor above bedrock (tests, shallow digs) */
    protected int floorLimit = Integer.MIN_VALUE;
    /** current tool position, synced for the renderers */
    @Nullable
    protected BlockPos head;
    // client-side interpolation
    @Nullable
    public BlockPos clientPrevHead;
    public long clientHeadTime;
    public int clientMoveTicks = 8;

    protected AbstractQuarryBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout, int capacity, int maxReceive) {
        super(type, pos, state, layout, capacity, maxReceive, 0);
        sides.autoEject = true;
    }

    // ------------------------------------------------------------------ subclass hooks

    /** Works out the dig area; returns false when there is none yet. Sets x0..z1, topY. */
    protected abstract boolean defineArea();

    /** Ticks per block before speed augments. */
    protected abstract int ticksPerBlock();

    /** Base energy for a block of hardness 0 (the cost grows with hardness). */
    protected abstract int baseCost();

    /** Columns the dig leaves alone (the laser tower). */
    protected boolean skipColumn(int x, int z) {
        return false;
    }

    /** Frame building for the frame quarry; true when finished. */
    protected boolean tickBuild() {
        return true;
    }

    /** Called while mining/restoring (frame repair, tower checks). Return false to pause. */
    protected boolean tickUpkeep() {
        return true;
    }

    /** Called once restoration is complete (frame removal). */
    protected void onRestored() {}

    // ------------------------------------------------------------------ inventory rules

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == S_BIT) return stack.getItem() instanceof DrillBitItem;
        if (slot == S_SIEVE) return stack.getItem() instanceof SieveItem;
        if (slot >= S_FILL && slot < S_FILL + FILLS) return stack.is(FILL);
        return super.isItemValid(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == S_BIT || slot == S_SIEVE ? 1 : super.getSlotLimit(slot);
    }

    @Override
    public boolean acceptsAugment(AugmentItem a) {
        return a.kind == AugmentItem.Kind.SPEED || a.kind == AugmentItem.Kind.EFFICIENCY || a.kind == AugmentItem.Kind.CAPACITY
                || a.kind == AugmentItem.Kind.RANGE;
    }

    // ------------------------------------------------------------------ area helpers

    public int width() {
        return x1 - x0 + 1;
    }

    public int depth() {
        return z1 - z0 + 1;
    }

    protected int columns() {
        return width() * depth();
    }

    /** Serpentine walk over the area: column index to world position. */
    protected int colX(int i) {
        int w = width();
        int row = i / w, col = i % w;
        return (row & 1) == 0 ? x0 + col : x1 - col;
    }

    protected int colZ(int i) {
        return z0 + i / width();
    }

    protected int columnOf(int x, int z) {
        return (z - z0) * width() + (x - x0);
    }

    protected void startArea() {
        hasArea = true;
        floorY = Math.max(level.getMinBuildHeight(), floorLimit);
        curY = topY;
        index = 0;
        bottomY = Integer.MAX_VALUE;
        surface = new int[columns()];
        Arrays.fill(surface, Integer.MIN_VALUE);
        topKind = new byte[columns()];
        head = null;
        sync();
    }

    public void setFloorLimit(int y) {
        floorLimit = y;
        if (hasArea && level != null) floorY = Math.max(level.getMinBuildHeight(), y);
    }

    public Phase phase() {
        return phase;
    }

    public int mined() {
        return mined;
    }

    public void setRestore(boolean on) {
        restore = on;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        flushPending();
        status = ST_OK;
        boolean active = false;
        if (!canRun()) {
            setLit(false);
            return;
        }
        switch (phase) {
            case NO_AREA -> {
                if (level.getGameTime() % 10 == 0 && energy.getEnergyStored() > 0 && defineArea()) {
                    startArea();
                    phase = Phase.BUILDING;
                    sync();
                }
            }
            case BUILDING -> {
                active = true;
                if (tickBuild()) {
                    phase = Phase.MINING;
                    sync();
                }
            }
            case BLOCKED -> {
                if (level.getGameTime() % 40 == 0) phase = Phase.BUILDING;
            }
            case MINING -> {
                if (tickUpkeep()) active = tickMining();
            }
            case RESTORING -> {
                if (tickUpkeep()) active = tickRestore();
            }
            case DONE -> {}
        }
        rate = active ? Math.max(1, lastCost / Math.max(1, ticksPerBlock())) : 0;
        setLit(active);
    }

    private double gain() {
        return 1.0 + augment(AugmentItem.Kind.SPEED);
    }

    /** Digs as many blocks as the accumulated work allows. Returns whether the quarry is busy. */
    protected boolean tickMining() {
        if (!pending.isEmpty() && !dropOverflow) {
            status = ST_FULL;
            return false;
        }
        int tpb = ticksPerBlock();
        work = Math.min(work + gain(), tpb * 4.0);
        int ops = 0;
        while (work >= tpb && ops < 4) {
            BlockPos target = nextTarget();
            if (target == null) return true;
            if (target == FINISHED) {
                finishMining();
                return false;
            }
            BlockState st = level.getBlockState(target);
            int cost = cost(st, target);
            if (energy.getEnergyStored() < cost) {
                status = ST_NO_POWER;
                return false;
            }
            energy.consume(cost);
            lastCost = cost;
            dig(target, st);
            advance();
            work -= tpb;
            ops++;
            moveHead(target, (int) Math.max(1, Math.round(tpb / gain())));
            if (!pending.isEmpty() && !dropOverflow) break;
        }
        return true;
    }

    private static final BlockPos FINISHED = new BlockPos(0, -10000, 0);

    /** Next block that needs digging; null when the search budget ran out this tick; FINISHED when the floor is passed. */
    @Nullable
    protected BlockPos nextTarget() {
        for (int budget = 0; budget < 512; budget++) {
            if (curY < floorY) return FINISHED;
            int x = colX(index), z = colZ(index);
            if (skipColumn(x, z)) {
                advance();
                continue;
            }
            BlockPos p = new BlockPos(x, curY, z);
            if (!level.isLoaded(p)) return null;
            BlockState st = level.getBlockState(p);
            if (needsDigging(st, p)) return p;
            advance();
        }
        return null;
    }

    protected boolean needsDigging(BlockState st, BlockPos p) {
        if (st.isAir()) return false;
        if (st.getBlock() instanceof LiquidBlock) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            return false;
        }
        if (st.is(IMMUNE) || st.hasBlockEntity()) return false;
        return st.getDestroySpeed(level, p) >= 0;
    }

    protected void advance() {
        if (++index >= columns()) {
            index = 0;
            curY--;
        }
    }

    protected int cost(BlockState st, BlockPos p) {
        float h = Math.min(50f, Math.max(0f, st.getDestroySpeed(level, p)));
        double c = baseCost() * (1.0 + h) * DrillBitItem.energy(items.getStackInSlot(S_BIT)) * energyMultiplier();
        return (int) Math.max(1, Math.round(c));
    }

    /** Breaks one block: rolls its loot with the drill bit, records the surface, keeps spoil or delivers the drops. */
    protected void dig(BlockPos p, BlockState st) {
        ServerLevel sl = (ServerLevel) level;
        FakePlayer fp = FakePlayerFactory.getMinecraft(sl);
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, p, st, fp))) return;
        List<ItemStack> drops = Block.getDrops(st, sl, p, null, fp, DrillBitItem.tool(items.getStackInSlot(S_BIT)));
        int col = columnOf(p.getX(), p.getZ());
        if (col >= 0 && col < surface.length && surface[col] == Integer.MIN_VALUE && st.isCollisionShapeFullBlock(level, p)
                && !st.is(BlockTags.LOGS) && !st.is(BlockTags.LEAVES)) {
            surface[col] = p.getY();
            topKind[col] = (byte) (st.is(BlockTags.SAND) ? 2 : st.is(SieveItem.SOIL) ? 1 : 3);
        }
        bottomY = Math.min(bottomY, p.getY());
        level.levelEvent(2001, p, Block.getId(st));
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        mined++;
        boolean soil = st.is(SieveItem.SOIL), rubble = st.is(SieveItem.RUBBLE);
        if (restore && (soil || rubble)) {
            if (soil) spoilSoil++;
            else spoilRubble++;
            return;
        }
        if (!SieveItem.keeps(items.getStackInSlot(S_SIEVE), st)) return;
        for (ItemStack d : drops) deliver(d);
    }

    protected void deliver(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack rem = insertOutput(stack, false);
        if (!rem.isEmpty()) pending.add(rem);
    }

    protected void flushPending() {
        if (pending.isEmpty()) return;
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack st : pending) {
            ItemStack rem = insertOutput(st, false);
            if (!rem.isEmpty()) left.add(rem);
        }
        pending.clear();
        if (dropOverflow) {
            for (ItemStack st : left) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, st);
            }
        } else {
            pending.addAll(left);
        }
    }

    protected void moveHead(BlockPos target, int ticks) {
        head = target;
        clientMoveTicks = ticks;
        sync();
    }

    protected void finishMining() {
        head = null;
        if (restore && bottomY != Integer.MAX_VALUE) {
            phase = Phase.RESTORING;
            curY = bottomY;
            index = 0;
        } else {
            phase = Phase.DONE;
        }
        sync();
    }

    // ------------------------------------------------------------------ restoration

    protected int surfaceTop() {
        int top = Integer.MIN_VALUE;
        for (int s : surface) top = Math.max(top, s);
        return top;
    }

    /** Block a restored position gets, from its depth below the recorded surface of its column. */
    protected BlockState fillState(int col, int y) {
        int depth = surface[col] - y;
        int kind = topKind[col];
        BlockState base = y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (kind == 1) return depth == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : depth <= 3 ? Blocks.DIRT.defaultBlockState() : base;
        if (kind == 2) return depth <= 2 ? Blocks.SAND.defaultBlockState() : depth == 3 ? Blocks.SANDSTONE.defaultBlockState() : base;
        return base;
    }

    private boolean takeMaterial(boolean soilWanted) {
        if (soilWanted && spoilSoil > 0) {
            spoilSoil--;
            return true;
        }
        if (spoilRubble > 0) {
            spoilRubble--;
            return true;
        }
        if (spoilSoil > 0) {
            spoilSoil--;
            return true;
        }
        for (int s = S_FILL; s < S_FILL + FILLS; s++) {
            if (!items.getStackInSlot(s).isEmpty()) {
                items.extractItem(s, 1, false);
                return true;
            }
        }
        return false;
    }

    /** Fills the hole from the bottom up, layer by layer, up to each column's old surface. */
    protected boolean tickRestore() {
        int top = surfaceTop();
        int budget = (int) Math.round(gain() * 2);
        int checks = 0;
        while (budget > 0 && checks++ < 512) {
            if (curY > top) {
                onRestored();
                phase = Phase.DONE;
                sync();
                return false;
            }
            int x = colX(index), z = colZ(index);
            int col = columnOf(x, z);
            BlockPos p = new BlockPos(x, curY, z);
            if (skipColumn(x, z) || surface[col] < curY) {
                advance2();
                continue;
            }
            BlockState here = level.getBlockState(p);
            if (!here.isAir() && !(here.getBlock() instanceof LiquidBlock)) {
                advance2();
                continue;
            }
            int cost = (int) Math.max(1, Math.round(20 * energyMultiplier()));
            if (energy.getEnergyStored() < cost) {
                status = ST_NO_POWER;
                return false;
            }
            BlockState fill = fillState(col, curY);
            boolean soil = fill.is(SieveItem.SOIL);
            if (!takeMaterial(soil)) {
                status = ST_NEEDS_FILL;
                return false;
            }
            energy.consume(cost);
            lastCost = cost;
            level.setBlock(p, fill, 3);
            moveHead(p, 2);
            advance2();
            budget--;
        }
        return true;
    }

    /** Restoration walks upward. */
    private void advance2() {
        if (++index >= columns()) {
            index = 0;
            curY++;
        }
    }

    // ------------------------------------------------------------------ buttons & screen data

    @Override
    public void onCustomButton(int id, Player player) {
        switch (id) {
            case BTN_RESTORE -> restore = !restore;
            case BTN_SHOW -> {
                show = !show;
                sync();
            }
            case BTN_OVERFLOW -> dropOverflow = !dropOverflow;
            case BTN_RESET -> reset();
            default -> {}
        }
    }

    /** Starts over from the top of a freshly worked-out area (keeps spoil). */
    protected void reset() {
        phase = Phase.NO_AREA;
        hasArea = false;
        head = null;
        mined = 0;
        sync();
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        int f = menu.extra(X_FLAGS);
        return switch (id) {
            case BTN_RESTORE -> Component.translatable((f & 1) != 0 ? "gui.ls_quarry.restore.on" : "gui.ls_quarry.restore.off");
            case BTN_SHOW -> Component.translatable((f & 2) != 0 ? "gui.ls_quarry.show.on" : "gui.ls_quarry.show.off");
            case BTN_OVERFLOW -> Component.translatable((f & 4) != 0 ? "gui.ls_quarry.overflow.drop" : "gui.ls_quarry.overflow.hold");
            default -> super.buttonLabel(id, menu);
        };
    }

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case X_PHASE -> phase.ordinal();
            case X_Y -> curY;
            case X_PROGRESS -> hasArea && columns() > 0 ? index * 1000 / columns() : 0;
            case X_MINED -> mined;
            case X_COST -> lastCost;
            case X_FLAGS -> (restore ? 1 : 0) | (show ? 2 : 0) | (dropOverflow ? 4 : 0);
            case X_SIZE_A -> hasArea ? width() : 0;
            case X_SIZE_B -> hasArea ? depth() : 0;
            case X_RUBBLE -> spoilRubble;
            case X_SOIL -> spoilSoil;
            case X_TOP -> topY;
            case X_FLOOR -> floorY;
            case X_STATUS -> status;
            default -> 0;
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        Phase ph = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, menu.extra(X_PHASE)))];
        net.minecraft.network.chat.MutableComponent first = Component.translatable("gui.ls_quarry.phase." + ph.name().toLowerCase(Locale.ROOT));
        int st = menu.extra(X_STATUS);
        String warn = switch (st) {
            case ST_NO_POWER -> "gui.ls_quarry.no_power";
            case ST_FULL -> "gui.ls_quarry.full";
            case ST_NEEDS_FILL -> "gui.ls_quarry.needs_fill";
            case ST_NO_TOWER -> "gui.ls_quarry.no_tower";
            default -> null;
        };
        lines.add(warn != null ? first.append(" - ").append(Component.translatable(warn).withStyle(ChatFormatting.DARK_RED)) : first);
        if (ph == Phase.MINING) {
            lines.add(Component.translatable("gui.ls_quarry.layer", menu.extra(X_Y), menu.extra(X_PROGRESS) / 10));
        } else if (ph == Phase.RESTORING) {
            lines.add(Component.translatable("gui.ls_quarry.restore_layer", menu.extra(X_Y)));
        } else if (menu.extra(X_SIZE_A) > 0) {
            lines.add(Component.translatable("gui.ls_quarry.size", menu.extra(X_SIZE_A), menu.extra(X_SIZE_B)));
        }
    }

    // ------------------------------------------------------------------ persistence & sync

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        for (ItemStack st : pending) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st);
        pending.clear();
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Phase", phase.ordinal());
        tag.putBoolean("HasArea", hasArea);
        tag.putIntArray("Area", new int[]{x0, z0, x1, z1, topY, floorY, curY, index});
        tag.putInt("Mined", mined);
        tag.putBoolean("Restore", restore);
        tag.putBoolean("Show", show);
        tag.putBoolean("Drop", dropOverflow);
        tag.putInt("SpoilRubble", spoilRubble);
        tag.putInt("SpoilSoil", spoilSoil);
        tag.putIntArray("Surface", surface);
        tag.putByteArray("TopKind", topKind);
        tag.putInt("Bottom", bottomY);
        tag.putInt("FloorLimit", floorLimit);
        tag.putDouble("Work", work);
        ListTag list = new ListTag();
        for (ItemStack st : pending) list.add(st.save(new CompoundTag()));
        tag.put("Pending", list);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, tag.getInt("Phase")))];
        hasArea = tag.getBoolean("HasArea");
        int[] a = tag.getIntArray("Area");
        if (a.length == 8) {
            x0 = a[0];
            z0 = a[1];
            x1 = a[2];
            z1 = a[3];
            topY = a[4];
            floorY = a[5];
            curY = a[6];
            index = a[7];
        }
        mined = tag.getInt("Mined");
        restore = tag.getBoolean("Restore");
        show = tag.getBoolean("Show");
        dropOverflow = tag.getBoolean("Drop");
        spoilRubble = tag.getInt("SpoilRubble");
        spoilSoil = tag.getInt("SpoilSoil");
        surface = tag.getIntArray("Surface");
        topKind = tag.getByteArray("TopKind");
        if (hasArea && surface.length != columns()) {
            surface = new int[columns()];
            Arrays.fill(surface, Integer.MIN_VALUE);
            topKind = new byte[columns()];
        }
        bottomY = tag.contains("Bottom") ? tag.getInt("Bottom") : Integer.MAX_VALUE;
        floorLimit = tag.contains("FloorLimit") ? tag.getInt("FloorLimit") : Integer.MIN_VALUE;
        work = tag.getDouble("Work");
        pending.clear();
        for (Tag t : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            ItemStack st = ItemStack.of((CompoundTag) t);
            if (!st.isEmpty()) pending.add(st);
        }
    }

    @Override
    protected void saveSyncExtra(CompoundTag tag) {
        tag.putInt("Phase", phase.ordinal());
        tag.putBoolean("HasArea", hasArea);
        tag.putIntArray("Area", new int[]{x0, z0, x1, z1, topY, floorY, curY});
        tag.putBoolean("Show", show);
        if (head != null) tag.put("Head", NbtUtils.writeBlockPos(head));
        tag.putInt("Move", clientMoveTicks);
    }

    @Override
    protected void loadSyncExtra(CompoundTag tag) {
        phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, tag.getInt("Phase")))];
        hasArea = tag.getBoolean("HasArea");
        int[] a = tag.getIntArray("Area");
        if (a.length == 7) {
            x0 = a[0];
            z0 = a[1];
            x1 = a[2];
            z1 = a[3];
            topY = a[4];
            floorY = a[5];
            curY = a[6];
        }
        show = tag.getBoolean("Show");
        BlockPos nh = tag.contains("Head") ? NbtUtils.readBlockPos(tag.getCompound("Head")) : null;
        if (nh != null && !nh.equals(head)) {
            clientPrevHead = head != null ? head : nh;
            clientHeadTime = level != null ? level.getGameTime() : 0;
        }
        head = nh;
        clientMoveTicks = Math.max(1, tag.getInt("Move"));
    }

    // ------------------------------------------------------------------ client accessors

    public boolean hasArea() {
        return hasArea;
    }

    public boolean showArea() {
        return show;
    }

    @Nullable
    public BlockPos head() {
        return head;
    }

    public int x0() {
        return x0;
    }

    public int x1() {
        return x1;
    }

    public int z0() {
        return z0;
    }

    public int z1() {
        return z1;
    }

    public int topY() {
        return topY;
    }

    public int floorY() {
        return floorY;
    }

    public int curY() {
        return curY;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (!hasArea) return new AABB(worldPosition).inflate(2);
        int minY = level != null ? level.getMinBuildHeight() : -64;
        return new AABB(Math.min(x0 - 1, worldPosition.getX()), minY, Math.min(z0 - 1, worldPosition.getZ()),
                Math.max(x1 + 2, worldPosition.getX() + 1), Math.max(topY + 6, worldPosition.getY() + 2), Math.max(z1 + 2, worldPosition.getZ() + 1));
    }
}
