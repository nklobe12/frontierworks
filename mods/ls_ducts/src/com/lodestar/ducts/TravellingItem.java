package com.lodestar.ducts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A stack moving through item ducts. It sits in one duct at a time, moving from that duct's centre towards the
 * next block along its path; past the last duct it enters the destination inventory through destFace.
 */
public class TravellingItem {
    public ItemStack stack;
    /** remaining duct positions after the current one */
    public final List<BlockPos> path;
    public BlockPos source;
    public Direction sourceFace;
    public Direction destFace;
    public float progress;
    public float speed;
    /** direction from the current duct to the next block (client rendering) */
    public Direction dir = Direction.NORTH;

    public TravellingItem(ItemStack stack, List<BlockPos> path, BlockPos source, Direction sourceFace, Direction destFace, float speed) {
        this.stack = stack;
        this.path = path;
        this.source = source;
        this.sourceFace = sourceFace;
        this.destFace = destFace;
        this.speed = speed;
    }

    public CompoundTag save(boolean full) {
        CompoundTag t = new CompoundTag();
        t.put("Stack", stack.save(new CompoundTag()));
        t.putFloat("P", progress);
        t.putFloat("S", speed);
        t.putByte("D", (byte) dir.get3DDataValue());
        if (full) {
            ListTag l = new ListTag();
            for (BlockPos p : path) l.add(NbtUtils.writeBlockPos(p));
            t.put("Path", l);
            t.put("Src", NbtUtils.writeBlockPos(source));
            t.putByte("SF", (byte) sourceFace.get3DDataValue());
            t.putByte("DF", (byte) destFace.get3DDataValue());
        }
        return t;
    }

    public static TravellingItem load(CompoundTag t) {
        List<BlockPos> path = new ArrayList<>();
        for (Tag e : t.getList("Path", Tag.TAG_COMPOUND)) path.add(NbtUtils.readBlockPos((CompoundTag) e));
        TravellingItem ti = new TravellingItem(ItemStack.of(t.getCompound("Stack")), path,
                t.contains("Src") ? NbtUtils.readBlockPos(t.getCompound("Src")) : BlockPos.ZERO,
                Direction.from3DDataValue(t.getByte("SF")), Direction.from3DDataValue(t.getByte("DF")), t.getFloat("S"));
        ti.progress = t.getFloat("P");
        ti.dir = Direction.from3DDataValue(t.getByte("D"));
        return ti;
    }
}
