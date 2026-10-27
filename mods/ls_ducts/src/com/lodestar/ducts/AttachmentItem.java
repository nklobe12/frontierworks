package com.lodestar.ducts;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Servo, filter or retriever, for item or fluid ducts. */
public class AttachmentItem extends Item {
    public final Attachment.Type type;
    public final boolean fluid;

    public AttachmentItem(Properties props, Attachment.Type type, boolean fluid) {
        super(props);
        this.type = type;
        this.fluid = fluid;
    }

    public boolean fits(DuctKind kind) {
        if (type == Attachment.Type.RETRIEVER) return kind == DuctKind.ITEM && !fluid;
        return fluid ? kind == DuctKind.FLUID : kind == DuctKind.ITEM;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String k = "tooltip.ls_ducts." + type.name().toLowerCase(java.util.Locale.ROOT) + (fluid ? "_fluid" : "");
        for (String s : Component.translatable(k).getString().split("\n")) tip.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }
}
