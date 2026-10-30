package com.lodestar.jetpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A jetpack upgrade, installed by crafting it together with a jetpack. */
public class UpgradeItem extends Item {
    public final JetpackItem.Mod mod;

    public UpgradeItem(Properties props, JetpackItem.Mod mod) {
        super(props);
        this.mod = mod;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.ls_jetpacks.max_level", mod.max).withStyle(ChatFormatting.DARK_GRAY));
    }
}
