package com.frontierworks.glider;

import com.frontierworks.core.net.CoreNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Hang glider: hold it while falling to glide forward in the direction you look. */
public class GliderItem extends Item {
    public final double sink;
    public final double speed;

    public GliderItem(Properties props, double sink, double speed) {
        super(props);
        this.sink = sink;
        this.speed = speed;
    }

    public static void init() {
        CoreNet.register("fw_glider:glide", (player, data) -> {
            player.fallDistance = 0;
            ItemStack st = player.getMainHandItem().getItem() instanceof GliderItem ? player.getMainHandItem() : player.getOffhandItem();
            if (st.getItem() instanceof GliderItem && player.tickCount % 40 == 0) {
                st.hurtAndBreak(1, player, p -> {});
            }
        });
    }

    public static ItemStack held(Player p) {
        if (p.getMainHandItem().getItem() instanceof GliderItem) return p.getMainHandItem();
        if (p.getOffhandItem().getItem() instanceof GliderItem) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_glider.use").withStyle(ChatFormatting.GRAY));
    }
}
