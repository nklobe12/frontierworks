package com.lodestar.quarry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Goes in a quarry's bit slot: the quarry mines as if with a Fortune or Silk Touch pickaxe, at a higher energy cost. */
public class DrillBitItem extends Item {
    public enum Kind { FORTUNE, SILK }

    public final Kind kind;
    public final int level;
    public final float energyMultiplier;

    public DrillBitItem(Properties props, Kind kind, int level, float energyMultiplier) {
        super(props);
        this.kind = kind;
        this.level = level;
        this.energyMultiplier = energyMultiplier;
    }

    /** The virtual pickaxe used to roll the loot of mined blocks. */
    public static ItemStack tool(ItemStack bit) {
        ItemStack t = new ItemStack(Items.NETHERITE_PICKAXE);
        if (bit.getItem() instanceof DrillBitItem b) {
            if (b.kind == Kind.SILK) t.enchant(Enchantments.SILK_TOUCH, 1);
            else t.enchant(Enchantments.BLOCK_FORTUNE, b.level);
        }
        return t;
    }

    public static float energy(ItemStack bit) {
        return bit.getItem() instanceof DrillBitItem b ? b.energyMultiplier : 1f;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (kind == Kind.SILK) tip.add(Component.translatable("tooltip.ls_quarry.bit.silk").withStyle(ChatFormatting.GRAY));
        else tip.add(Component.translatable("tooltip.ls_quarry.bit.fortune", "I".repeat(Math.max(1, this.level))).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.ls_quarry.bit.energy", String.format("%.1f", energyMultiplier)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.ls_quarry.bit.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
