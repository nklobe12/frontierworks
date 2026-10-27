package com.frontierworks.bounties;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class ContractItem extends Item {
    public ContractItem(Properties props) {
        super(props);
    }

    public static String target(ItemStack s) {
        return s.getOrCreateTag().getString("Target");
    }

    public static int needed(ItemStack s) {
        return s.getOrCreateTag().getInt("Needed");
    }

    public static int progress(ItemStack s) {
        return s.getOrCreateTag().getInt("Progress");
    }

    public static int tier(ItemStack s) {
        return Math.max(1, s.getOrCreateTag().getInt("Tier"));
    }

    public static boolean complete(ItemStack s) {
        return needed(s) > 0 && progress(s) >= needed(s);
    }

    public static Component targetName(String id) {
        EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        return t == null ? Component.literal(id) : t.getDescription();
    }

    public static ItemStack create(String target, int needed, int tier) {
        ItemStack s = new ItemStack(Content.BOUNTY_CONTRACT.get());
        CompoundTag t = s.getOrCreateTag();
        t.putString("Target", target);
        t.putInt("Needed", needed);
        t.putInt("Progress", 0);
        t.putInt("Tier", tier);
        return s;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return complete(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (target(stack).isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.fw_bounties.target", targetName(target(stack)), progress(stack), needed(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.fw_bounties.tier", Component.translatable("tier.fw_bounties." + tier(stack))).withStyle(ChatFormatting.GRAY));
        if (complete(stack)) tooltip.add(Component.translatable("tooltip.fw_bounties.done").withStyle(ChatFormatting.GREEN));
    }
}
