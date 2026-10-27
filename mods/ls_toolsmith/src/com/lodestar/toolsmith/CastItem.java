package com.lodestar.toolsmith;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A gold cast of a shape (a tool part, ingot, nugget, plate, gear or rod). Placed on a casting table; never used up. */
public class CastItem extends Item {
    public final String shape;
    /** molten metal needed to fill the cast */
    public final int amount;

    public CastItem(Properties props, String shape, int amount) {
        super(props.stacksTo(16));
        this.shape = shape;
        this.amount = amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.ls_toolsmith.cast", amount).withStyle(ChatFormatting.GRAY));
    }
}
