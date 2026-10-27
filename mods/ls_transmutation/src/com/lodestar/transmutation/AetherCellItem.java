package com.lodestar.transmutation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Portable aether storage. Charged in relays and the exchange table; tools, rings and armor draw from it. */
public class AetherCellItem extends Item {
    public final long capacity;

    public AetherCellItem(Properties props, long capacity) {
        super(props.stacksTo(1));
        this.capacity = capacity;
    }

    public static long stored(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getLong("Aether") : 0;
    }

    public static void set(ItemStack stack, long v) {
        stack.getOrCreateTag().putLong("Aether", Math.max(0, v));
    }

    /** Adds aether, returns how much fitted. */
    public static long charge(ItemStack stack, long amount) {
        if (!(stack.getItem() instanceof AetherCellItem c)) return 0;
        long put = Math.min(amount, c.capacity - stored(stack));
        if (put > 0) set(stack, stored(stack) + put);
        return Math.max(0, put);
    }

    /** Takes aether from the player's cells; returns whether the full amount was paid. */
    public static boolean consume(Player player, long amount) {
        if (player.isCreative() || amount <= 0) return true;
        long have = 0;
        for (ItemStack st : player.getInventory().items) if (st.getItem() instanceof AetherCellItem) have += stored(st);
        if (have < amount) return false;
        long left = amount;
        for (ItemStack st : player.getInventory().items) {
            if (left <= 0) break;
            if (!(st.getItem() instanceof AetherCellItem)) continue;
            long take = Math.min(left, stored(st));
            set(st, stored(st) - take);
            left -= take;
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0 * stored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x7FD4FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.ls_transmutation.cell", AetherValues.format(stored(stack)), AetherValues.format(capacity))
                .withStyle(ChatFormatting.AQUA));
    }
}
