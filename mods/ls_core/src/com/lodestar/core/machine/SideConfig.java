package com.lodestar.core.machine;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Per-face automation settings of a machine, stored relative to the machine's facing so they survive rotation.
 * Faces: 0 front, 1 back, 2 left, 3 right, 4 top, 5 bottom.
 */
public class SideConfig {
    public enum Mode {
        NONE, INPUT, OUTPUT, BOTH;

        public boolean in() {
            return this == INPUT || this == BOTH;
        }

        public boolean out() {
            return this == OUTPUT || this == BOTH;
        }
    }

    public static final int FRONT = 0, BACK = 1, LEFT = 2, RIGHT = 3, TOP = 4, BOTTOM = 5;
    public static final String[] FACE_NAMES = {"front", "back", "left", "right", "top", "bottom"};

    private final Mode[] modes = new Mode[6];
    public boolean autoEject;
    public boolean autoInput;

    public SideConfig() {
        for (int i = 0; i < 6; i++) modes[i] = Mode.BOTH;
    }

    public Mode get(int face) {
        return modes[face];
    }

    public void set(int face, Mode m) {
        modes[face] = m;
    }

    public void cycle(int face, boolean forward) {
        int n = Mode.values().length;
        modes[face] = Mode.values()[(modes[face].ordinal() + (forward ? 1 : n - 1)) % n];
    }

    /** Relative face index for an absolute side given the machine's facing. */
    public static int face(Direction facing, Direction side) {
        if (side == Direction.UP) return TOP;
        if (side == Direction.DOWN) return BOTTOM;
        if (side == facing) return FRONT;
        if (side == facing.getOpposite()) return BACK;
        return side == facing.getClockWise() ? LEFT : RIGHT;
    }

    /** Absolute side for a relative face. */
    public static Direction side(Direction facing, int face) {
        return switch (face) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            default -> Direction.DOWN;
        };
    }

    public Mode mode(Direction facing, Direction side) {
        return side == null ? Mode.BOTH : modes[face(facing, side)];
    }

    public int pack() {
        int v = 0;
        for (int i = 0; i < 6; i++) v |= modes[i].ordinal() << (i * 2);
        if (autoEject) v |= 1 << 12;
        if (autoInput) v |= 1 << 13;
        return v;
    }

    public void unpack(int v) {
        for (int i = 0; i < 6; i++) modes[i] = Mode.values()[(v >> (i * 2)) & 3];
        autoEject = (v & (1 << 12)) != 0;
        autoInput = (v & (1 << 13)) != 0;
    }

    public void save(CompoundTag tag) {
        tag.putInt("Sides", pack());
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Sides")) unpack(tag.getInt("Sides"));
    }
}
