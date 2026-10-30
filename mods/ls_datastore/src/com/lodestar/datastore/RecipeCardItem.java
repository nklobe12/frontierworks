package com.lodestar.datastore;

import com.lodestar.datastore.craft.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An encoded recipe card: a crafting recipe written by the pattern encoder, run by fabricators. */
public class RecipeCardItem extends Item {
    public RecipeCardItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack out = Pattern.outputOf(stack);
        if (out.isEmpty()) return super.getName(stack);
        return Component.translatable("item.ls_datastore.recipe_card").append(": ").append(out.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = stack.getTag();
        if (t == null) return;
        ItemStack out = Pattern.outputOf(stack);
        tip.add(Component.translatable("tooltip.ls_datastore.card.makes", out.getCount(), out.getHoverName()).withStyle(ChatFormatting.AQUA));
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Component> names = new LinkedHashMap<>();
        ListTag g = t.getList("Grid", Tag.TAG_COMPOUND);
        for (int i = 0; i < g.size(); i++) {
            ItemStack s = ItemStack.of(g.getCompound(i));
            if (s.isEmpty()) continue;
            String k = s.getDescriptionId();
            counts.merge(k, 1, Integer::sum);
            names.putIfAbsent(k, s.getHoverName());
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            tip.add(Component.translatable("tooltip.ls_datastore.card.from", e.getValue(), names.get(e.getKey())).withStyle(ChatFormatting.GRAY));
        }
        if (t.getBoolean("Sub")) tip.add(Component.translatable("tooltip.ls_datastore.card.sub").withStyle(ChatFormatting.DARK_AQUA));
    }
}
