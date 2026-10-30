package com.lodestar.backpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A module fitted in a backpack's upgrade slots. */
public class UpgradeItem extends Item {
    public enum Kind { MAGNET, PICKUP, FEEDING, REFILL, CRAFTING, SMELTING, TANK }

    public final Kind kind;

    public UpgradeItem(Properties props, Kind kind) {
        super(props.stacksTo(1));
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (String s : Component.translatable("tooltip.ls_backpacks." + kind.name().toLowerCase(java.util.Locale.ROOT)).getString().split("\n")) {
            tip.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
        }
    }
}
