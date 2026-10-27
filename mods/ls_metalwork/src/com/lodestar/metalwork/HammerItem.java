package com.lodestar.metalwork;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A crafting tool that survives crafting (losing durability): smithing hammer, wire cutters, tongs. */
public class HammerItem extends Item {
    private final String hint;

    public HammerItem(Properties props, String hint) {
        super(props);
        this.hint = hint;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (copy.isDamageableItem()) {
            copy.setDamageValue(copy.getDamageValue() + 1);
            if (copy.getDamageValue() >= copy.getMaxDamage()) return ItemStack.EMPTY;
        }
        return copy;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (String line : Component.translatable(hint).getString().split("\n")) {
            tip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
