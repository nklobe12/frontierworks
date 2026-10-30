package com.lodestar.backpacks;

/** Backpack sizes: rows of nine slots and how many upgrade modules fit. */
public enum BackpackTier {
    LEATHER(3, 1), IRON(4, 2), GOLD(5, 3), DIAMOND(6, 4), NETHERITE(8, 5);

    public final int rows, upgrades;

    BackpackTier(int rows, int upgrades) {
        this.rows = rows;
        this.upgrades = upgrades;
    }

    public int slots() {
        return rows * 9;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
