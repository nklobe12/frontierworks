package com.lodestar.toolsmith;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A tool part of some material (stored as "Material" in NBT), tinted with the material's colour. */
public class PartItem extends Item {
    public final PartType part;

    public PartItem(Properties props, PartType part) {
        super(props);
        this.part = part;
    }

    public static ItemStack of(PartType part, ToolMaterial mat) {
        ItemStack st = new ItemStack(ToolsmithSetup.partItem(part));
        st.getOrCreateTag().putString("Material", mat.id());
        return st;
    }

    public static ToolMaterial material(ItemStack stack) {
        if (stack.getTag() == null) return Materials.FALLBACK;
        ToolMaterial m = Materials.get(stack.getTag().getString("Material"));
        return m == null ? Materials.FALLBACK : m;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("part.ls_toolsmith.named", material(stack).displayName(), part.displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        ToolMaterial m = material(stack);
        tip.add(Component.translatable("trait.ls_toolsmith." + m.trait()).withStyle(ChatFormatting.AQUA));
        switch (part.role) {
            case HEAD -> {
                tip.add(Component.translatable("tooltip.ls_toolsmith.part.durability", m.headDurability()).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("tooltip.ls_toolsmith.speed", String.format("%.1f", m.headSpeed())).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("tooltip.ls_toolsmith.part.attack", String.format("%.1f", m.headAttack())).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("tooltip.ls_toolsmith.tier", Component.translatable("tier.ls_toolsmith." + m.tier())).withStyle(ChatFormatting.GRAY));
            }
            case HANDLE -> tip.add(Component.translatable("tooltip.ls_toolsmith.part.handle", String.format("%.2f", m.handleMultiplier())).withStyle(ChatFormatting.GRAY));
            case BINDING -> tip.add(Component.translatable("tooltip.ls_toolsmith.part.binding", m.bindingDurability()).withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.translatable("tooltip.ls_toolsmith.part.cost", part.cost).withStyle(ChatFormatting.DARK_GRAY));
    }
}
