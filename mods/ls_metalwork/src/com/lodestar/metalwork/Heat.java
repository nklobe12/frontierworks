package com.lodestar.metalwork;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Heat carried by a workpiece (any item), stored as "ls_heat" in degrees Celsius. Hot items cool down over time,
 * burn bare hands and are forged on the smithing anvil. Below 100 C the tag is removed so stacks merge again.
 */
public final class Heat {
    public static final String TAG = "ls_heat";
    public static final int MAX = 1600;
    /** colors from dull red to white for renderers and tooltips */
    private static final int[] GLOW = {0x5A2018, 0x8E2A14, 0xC8401A, 0xF06A1E, 0xFFA040, 0xFFD27A, 0xFFF4D8};

    private Heat() {}

    public static int get(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 0 : t.getInt(TAG);
    }

    public static void set(ItemStack stack, int heat) {
        if (heat < 100) {
            CompoundTag t = stack.getTag();
            if (t != null) {
                t.remove(TAG);
                if (t.isEmpty()) stack.setTag(null);
            }
            return;
        }
        stack.getOrCreateTag().putInt(TAG, Math.min(MAX, heat));
    }

    public static boolean isHot(ItemStack stack) {
        return get(stack) >= 100;
    }

    /** Workable heat bands: below 600 too cold, 600-899 workable, 900+ ideal. */
    public static int band(int heat) {
        return heat < 600 ? 0 : heat < 900 ? 1 : 2;
    }

    public static int glowColor(int heat) {
        if (heat < 400) return 0;
        float f = Math.min(1f, (heat - 400) / 1200f) * (GLOW.length - 1);
        int i = (int) f;
        if (i >= GLOW.length - 1) return GLOW[GLOW.length - 1];
        float t = f - i;
        int a = GLOW[i], b = GLOW[i + 1];
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static Component describe(int heat) {
        String band = band(heat) == 0 ? "cold" : band(heat) == 1 ? "workable" : "ideal";
        ChatFormatting col = band(heat) == 0 ? ChatFormatting.GRAY : band(heat) == 1 ? ChatFormatting.GOLD : ChatFormatting.RED;
        return Component.translatable("tooltip.ls_metalwork.heat", heat, Component.translatable("tooltip.ls_metalwork.heat." + band)).withStyle(col);
    }
}
