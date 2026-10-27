package com.frontierworks.core.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Machine upgrade card. Placed in a machine's upgrade slots (up to 4 per slot). */
public class UpgradeItem extends Item {
    public enum Type { SPEED, EFFICIENCY, CAPACITY, RANGE }

    public final Type type;

    public UpgradeItem(Properties props, Type type) {
        super(props.stacksTo(16));
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_core.upgrade." + type.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
    }
}
