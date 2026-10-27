package com.lodestar.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * State kept by a multiblock controller: re-checks the pattern every {@code interval} ticks (or when asked),
 * tells part block entities about the controller, and remembers positions by character.
 */
public class MultiblockHelper {
    private final MultiblockPattern pattern;
    private final int interval;
    @Nullable
    private MultiblockPattern.Match match;
    private final List<BlockPos> parts = new ArrayList<>();
    private int timer;
    private boolean checkNow = true;
    private boolean wasFormed;

    public MultiblockHelper(MultiblockPattern pattern, int interval) {
        this.pattern = pattern;
        this.interval = interval;
    }

    public MultiblockPattern pattern() {
        return pattern;
    }

    public boolean formed() {
        return match != null;
    }

    @Nullable
    public MultiblockPattern.Match match() {
        return match;
    }

    public void requestCheck() {
        checkNow = true;
    }

    /** Call every server tick. Returns true when the formed state changed this tick. */
    public boolean tick(Level level, BlockPos ctrl, Direction facing) {
        if (!checkNow && ++timer < interval) return false;
        timer = 0;
        checkNow = false;
        MultiblockPattern.Match m = pattern.match(level, ctrl, facing);
        boolean changed = (m != null) != wasFormed || (m != null && match != null && !m.all.equals(match.all));
        if (changed) {
            for (BlockPos p : parts) setPart(level, p, null);
            parts.clear();
            if (m != null) {
                for (BlockPos p : m.all) {
                    if (p.equals(ctrl)) continue;
                    if (setPart(level, p, ctrl)) parts.add(p);
                }
            }
        }
        match = m;
        wasFormed = m != null;
        return changed;
    }

    /** Call when the controller is removed. */
    public void release(Level level) {
        for (BlockPos p : parts) setPart(level, p, null);
        parts.clear();
        match = null;
        wasFormed = false;
    }

    private static boolean setPart(Level level, BlockPos p, @Nullable BlockPos ctrl) {
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof IMultiblockPart part) {
            part.setController(ctrl);
            return true;
        }
        return false;
    }

    public List<BlockPos> positions(char c) {
        return match == null ? List.of() : match.get(c);
    }

    public void save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (BlockPos p : parts) l.add(NbtUtils.writeBlockPos(p));
        tag.put("MbParts", l);
    }

    public void load(CompoundTag tag) {
        parts.clear();
        for (Tag t : tag.getList("MbParts", Tag.TAG_COMPOUND)) parts.add(NbtUtils.readBlockPos((CompoundTag) t));
        checkNow = true;
    }
}
