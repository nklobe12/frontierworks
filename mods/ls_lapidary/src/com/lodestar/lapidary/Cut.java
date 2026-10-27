package com.lodestar.lapidary;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Gem cuts. Harder cuts take longer, roll a lower and wider quality and may shatter the stone, but multiply the bonus
 * the gem gives more.
 */
public enum Cut {
    CABOCHON(0.8f, 60, 15, 0.0f, 0.7f),
    STEP(1.0f, 48, 24, 0.04f, 1.0f),
    BRILLIANT(1.3f, 36, 34, 0.12f, 1.5f);

    /** bonus multiplier */
    public final float mult;
    /** quality before abrasive and augments */
    public final int base;
    /** +/- quality spread (triangular) */
    public final int spread;
    /** chance the stone shatters, before abrasive */
    public final float shatter;
    /** time factor on the recipe time */
    public final float time;

    Cut(float mult, int base, int spread, float shatter, float time) {
        this.mult = mult;
        this.base = base;
        this.spread = spread;
        this.shatter = shatter;
        this.time = time;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("cut.ls_lapidary." + id());
    }

    public static Cut byId(String id) {
        for (Cut c : values()) if (c.id().equals(id)) return c;
        return STEP;
    }

    public static Cut byIndex(int i) {
        return values()[Math.max(0, Math.min(values().length - 1, i))];
    }
}
