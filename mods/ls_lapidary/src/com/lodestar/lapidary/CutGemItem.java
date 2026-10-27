package com.lodestar.lapidary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A cut gem. Its cut and quality live in NBT and decide how strong it is once socketed. */
public class CutGemItem extends Item {
    public final GemType gem;

    public CutGemItem(Properties props, GemType gem) {
        super(props);
        this.gem = gem;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!GemData.isCut(stack)) return super.getName(stack);
        return Component.translatable("item.ls_lapidary.cut_gem", GemData.grade(GemData.quality(stack)), GemData.cut(stack).displayName(),
                Component.translatable(getDescriptionId()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return GemData.isCut(stack) && GemData.quality(stack) >= 95;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        Cut cut = GemData.cut(stack);
        int q = GemData.quality(stack);
        if (GemData.isCut(stack)) {
            tip.add(Component.translatable("tooltip.ls_lapidary.quality", q, cut.displayName()).withStyle(ChatFormatting.GRAY));
        }
        float s = GemData.scale(cut, q);
        tip.add(Component.translatable("tooltip.ls_lapidary.in_tools").withStyle(ChatFormatting.DARK_GRAY)
                .append(gem.toolEffect.describe(gem.toolEffect.perScale * s).withStyle(ChatFormatting.BLUE)));
        tip.add(Component.translatable("tooltip.ls_lapidary.in_armor").withStyle(ChatFormatting.DARK_GRAY)
                .append(gem.armorEffect.describe(gem.armorEffect.perScale * s).withStyle(ChatFormatting.BLUE)));
        tip.add(Component.translatable("tooltip.ls_lapidary.socket_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
