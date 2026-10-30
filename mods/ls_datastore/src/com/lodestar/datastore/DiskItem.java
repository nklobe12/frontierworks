package com.lodestar.datastore;

import com.lodestar.datastore.storage.DiskData;
import com.lodestar.datastore.storage.DiskStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A data disk. Its items live in {@link DiskData} under the disk's UUID (assigned on first use); the item keeps a small
 * "Used"/"Types" summary that drive bays refresh, for tooltips and the drive bay lights.
 */
public class DiskItem extends Item {
    public final long bytes;
    public final int types;

    public DiskItem(Properties props, long bytes, int types) {
        super(props);
        this.bytes = bytes;
        this.types = types;
    }

    public int bytesPerType() {
        return (int) Math.max(8, bytes / 256);
    }

    @Nullable
    public static UUID id(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.hasUUID("DiskId") ? t.getUUID("DiskId") : null;
    }

    public static UUID ensureId(ItemStack s) {
        UUID id = id(s);
        if (id == null) {
            id = UUID.randomUUID();
            s.getOrCreateTag().putUUID("DiskId", id);
        }
        return id;
    }

    /** Writes the tooltip summary; returns true if it changed. */
    public static boolean updateSummary(ItemStack s, DiskData.DiskInv inv) {
        if (!(s.getItem() instanceof DiskItem d)) return false;
        long used = DiskStorage.used(inv, d);
        int ty = inv.types();
        CompoundTag t = s.getOrCreateTag();
        if (t.getLong("Used") == used && t.getInt("Types") == ty) return false;
        t.putLong("Used", used);
        t.putInt("Types", ty);
        return true;
    }

    /** 0..1 fill from the summary (client safe). */
    public static float fill(ItemStack s) {
        if (!(s.getItem() instanceof DiskItem d) || s.getTag() == null) return 0;
        float b = (float) s.getTag().getLong("Used") / d.bytes;
        float ty = (float) s.getTag().getInt("Types") / d.types;
        return Math.max(b, ty);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = stack.getTag();
        long used = t == null ? 0 : t.getLong("Used");
        int ty = t == null ? 0 : t.getInt("Types");
        if (ty == 0) tip.add(Component.translatable("tooltip.ls_datastore.disk.empty").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("tooltip.ls_datastore.disk.bytes", String.format("%,d", used), String.format("%,d", bytes)).withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.ls_datastore.disk.types", ty, types).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getInt("Types") > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * Math.min(1f, fill(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = fill(stack);
        return f >= 1f ? 0xE04040 : f > 0.75f ? 0xE0C040 : 0x3FD0C9;
    }
}
