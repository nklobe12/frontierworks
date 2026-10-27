package com.lodestar.core.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes the inventory, tanks, buttons and screen layout of a machine. Shared by the server
 * (inventory roles) and client (procedural screen drawing) so no per-machine GUI code is needed.
 */
public class MachineLayout {
    public enum Role { INPUT, OUTPUT, FUEL, BATTERY, CHARGE, UPGRADE, STORAGE, FILTER }

    public static class SlotDef {
        public final int x, y;
        public final Role role;

        public SlotDef(int x, int y, Role role) {
            this.x = x;
            this.y = y;
            this.role = role;
        }
    }

    public static class TankDef {
        public final int x, y, w, h, capacity;
        public final boolean input, output;

        public TankDef(int x, int y, int w, int h, int capacity, boolean input, boolean output) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.capacity = capacity;
            this.input = input;
            this.output = output;
        }
    }

    public static class ButtonDef {
        public final int id, x, y, w, h;
        public final String labelKey;

        public ButtonDef(int id, int x, int y, int w, int h, String labelKey) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.labelKey = labelKey;
        }
    }

    public final List<SlotDef> slots;
    public final List<TankDef> tanks;
    public final List<ButtonDef> buttons;
    public final boolean energyBar;
    public final int energyX, energyY, energyH;
    public final int arrowX, arrowY;
    public final int flameX, flameY;
    public final int infoX, infoY;
    public final int extraData;
    public final int height;
    public final int invY;

    private MachineLayout(Builder b) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(b.slots));
        this.tanks = Collections.unmodifiableList(new ArrayList<>(b.tanks));
        this.buttons = Collections.unmodifiableList(new ArrayList<>(b.buttons));
        this.energyBar = b.energyBar;
        this.energyX = b.energyX;
        this.energyY = b.energyY;
        this.energyH = b.energyH;
        this.arrowX = b.arrowX;
        this.arrowY = b.arrowY;
        this.flameX = b.flameX;
        this.flameY = b.flameY;
        this.infoX = b.infoX;
        this.infoY = b.infoY;
        this.extraData = b.extraData;
        this.height = b.height;
        this.invY = b.height - 82;
    }

    public int count(Role role) {
        int n = 0;
        for (SlotDef d : slots) if (d.role == role) n++;
        return n;
    }

    public int[] indices(Role role) {
        int[] out = new int[count(role)];
        int j = 0;
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).role == role) out[j++] = i;
        return out;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Standard processing layout: energy bar on the left, inputs, arrow, outputs, optional fluid tanks,
     * upgrade slots on the right and an optional battery slot.
     */
    public static MachineLayout processing(int inputs, int outputs, int tanksIn, int tanksOut, int tankCapacity, boolean energy, int upgrades, boolean battery) {
        Builder b = builder();
        int x = 8;
        if (energy) {
            b.energy(8, 17, battery ? 38 : 52);
            if (battery) b.slot(9, 59, Role.BATTERY);
            x = 26;
        }
        for (int t = 0; t < tanksIn; t++) {
            b.tank(x, 17, 16, 52, tankCapacity, true, false);
            x += 20;
        }
        int inputY = inputs > 2 ? 26 : 35;
        int perRow = inputs > 2 ? 2 : inputs;
        int startX = x + 6;
        for (int i = 0; i < inputs; i++) {
            int col = i % Math.max(1, perRow);
            int row = i / Math.max(1, perRow);
            b.slot(startX + col * 18, inputY + row * 18, Role.INPUT);
        }
        x = startX + Math.max(1, perRow) * 18 + 4;
        b.arrow(x, 34);
        x += 28;
        int outPerRow = outputs > 2 ? 2 : Math.max(1, outputs);
        int outY = outputs > 2 ? 26 : 35;
        for (int o = 0; o < outputs; o++) {
            int col = o % outPerRow;
            int row = o / outPerRow;
            b.slot(x + col * 18, outY + row * 18, Role.OUTPUT);
        }
        x += outPerRow * 18 + 4;
        for (int t = 0; t < tanksOut; t++) {
            b.tank(x, 17, 16, 52, tankCapacity, false, true);
            x += 20;
        }
        for (int u = 0; u < upgrades; u++) {
            b.slot(152, 17 + u * 18, Role.UPGRADE);
        }
        return b.build();
    }

    /** Furnace-style layout: inputs on top, fuel slot below with flame meter, arrow, outputs. */
    public static MachineLayout fuel(int inputs, int outputs) {
        Builder b = builder();
        int x = inputs == 1 ? 56 : 38;
        for (int i = 0; i < inputs; i++) {
            b.slot(x + i * 18, 17, Role.INPUT);
        }
        int center = inputs == 1 ? 56 : 47;
        b.slot(center, 53, Role.FUEL);
        b.flame(center + 1, 36);
        b.arrow(79, 34);
        for (int o = 0; o < outputs; o++) {
            b.slot(outputs == 1 ? 116 : 112 + o * 18, 35, Role.OUTPUT);
        }
        return b.build();
    }

    /** Generator layout: optional fuel slot with flame, optional input tank, energy bar, charge slot. */
    public static MachineLayout generator(boolean fuelSlot, int tanks, int tankCap) {
        Builder b = builder();
        b.energy(156, 17, 52);
        if (fuelSlot) {
            b.slot(80, 53, Role.FUEL);
            b.flame(81, 35);
        }
        for (int t = 0; t < tanks; t++) {
            b.tank(26 + t * 20, 17, 16, 52, tankCap, true, false);
        }
        b.slot(130, 53, Role.CHARGE);
        b.info(tanks > 0 ? 26 + tanks * 20 + 4 : 30, 20);
        b.extra(1);
        return b.build();
    }

    public static class Builder {
        private final List<SlotDef> slots = new ArrayList<>();
        private final List<TankDef> tanks = new ArrayList<>();
        private final List<ButtonDef> buttons = new ArrayList<>();
        private boolean energyBar;
        private int energyX = 8, energyY = 17, energyH = 52;
        private int arrowX = -1, arrowY = -1, flameX = -1, flameY = -1;
        private int infoX = -1, infoY = -1;
        private int extraData;
        private int height = 166;

        public Builder slot(int x, int y, Role role) {
            slots.add(new SlotDef(x, y, role));
            return this;
        }

        public Builder grid(int x, int y, int cols, int rows, Role role) {
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    slot(x + c * 18, y + r * 18, role);
            return this;
        }

        public Builder tank(int x, int y, int w, int h, int capacity, boolean in, boolean out) {
            tanks.add(new TankDef(x, y, w, h, capacity, in, out));
            return this;
        }

        public Builder button(int id, int x, int y, int w, int h, String labelKey) {
            buttons.add(new ButtonDef(id, x, y, w, h, labelKey));
            return this;
        }

        public Builder energy(int x, int y, int h) {
            energyBar = true;
            energyX = x;
            energyY = y;
            energyH = h;
            return this;
        }

        public Builder arrow(int x, int y) {
            arrowX = x;
            arrowY = y;
            return this;
        }

        public Builder flame(int x, int y) {
            flameX = x;
            flameY = y;
            return this;
        }

        public Builder info(int x, int y) {
            infoX = x;
            infoY = y;
            return this;
        }

        public Builder extra(int n) {
            extraData = n;
            return this;
        }

        public Builder height(int h) {
            height = h;
            return this;
        }

        public MachineLayout build() {
            return new MachineLayout(this);
        }
    }
}
