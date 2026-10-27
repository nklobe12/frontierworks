package com.lodestar.veins;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** A printed seismic survey. Right-click to read the depth chart; the tooltip lists the richest ores. */
public class SurveyReportItem extends Item {
    public SurveyReportItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && stack.hasTag()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lodestar.veins.client.VeinsClient.openReport(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static long total(int[] a) {
        long t = 0;
        for (int v : a) t += v;
        return t;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tip.add(Component.translatable("tooltip.ls_veins.report.at", tag.getInt("X"), tag.getInt("Z")).withStyle(ChatFormatting.GRAY));
        CompoundTag ores = tag.getCompound("Ores");
        List<String> keys = new ArrayList<>(ores.getAllKeys());
        keys.sort((a, b) -> Long.compare(total(ores.getIntArray(b)), total(ores.getIntArray(a))));
        for (int i = 0; i < Math.min(5, keys.size()); i++) {
            String k = keys.get(i);
            tip.add(Component.literal("  ").append(OreIndex.display(k)).append(": " + total(ores.getIntArray(k)))
                    .withStyle(s -> s.withColor(OreIndex.color(k))));
        }
        if (keys.isEmpty()) tip.add(Component.translatable("tooltip.ls_veins.report.empty").withStyle(ChatFormatting.DARK_GRAY));
    }
}
