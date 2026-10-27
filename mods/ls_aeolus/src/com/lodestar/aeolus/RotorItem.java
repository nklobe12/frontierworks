package com.lodestar.aeolus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A wind turbine rotor. Its durability counts seconds of running; the turbine wears it while it turns (twice as fast in
 * thunderstorms) and it breaks when worn out. Repairable on an anvil with its material.
 */
public class RotorItem extends Item {
    public final int radius;
    public final int blades;
    public final int power;
    private final TagKey<Item> repair;

    public RotorItem(Properties props, int radius, int blades, int power, String repairTag) {
        super(props);
        this.radius = radius;
        this.blades = blades;
        this.power = power;
        this.repair = ItemTags.create(new ResourceLocation(repairTag));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
        return material.is(repair);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    public static String duration(int seconds) {
        if (seconds >= 3600) {
            int h = seconds / 3600, m = (seconds % 3600) / 60;
            return m == 0 ? h + "h" : h + "h " + m + "m";
        }
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ls_aeolus.rotor", radius, power).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ls_aeolus.rotor_life", duration(stack.getMaxDamage())).withStyle(ChatFormatting.GRAY));
        if (stack.isDamaged()) {
            tooltip.add(Component.translatable("tooltip.ls_aeolus.rotor_left", duration(stack.getMaxDamage() - stack.getDamageValue()))
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.ls_aeolus.rotor_repair").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
