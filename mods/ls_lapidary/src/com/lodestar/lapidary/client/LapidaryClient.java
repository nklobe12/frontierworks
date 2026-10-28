package com.lodestar.lapidary.client;

import com.lodestar.core.client.MachineScreen;
import com.lodestar.lapidary.Content;
import com.lodestar.lapidary.CutGemItem;
import com.lodestar.lapidary.GemData;
import com.lodestar.lapidary.GemEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Client setup for Lapidary: machine screens, cut-dependent gem models and socket tooltips. */
public final class LapidaryClient {
    private LapidaryClient() {}

    public static void init(IEventBus bus) {
        bus.addListener(LapidaryClient::setup);
        MinecraftForge.EVENT_BUS.addListener(LapidaryClient::tooltip);
    }

    private static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            MachineScreen.register(Content.CUTTING_WHEEL_BE.get(), CuttingWheelScreen::new);
            MachineScreen.register(Content.JEWELERS_TABLE_BE.get(), JewelerScreen::new);
            ResourceLocation cut = new ResourceLocation("ls_lapidary", "cut");
            for (var item : ForgeRegistries.ITEMS.getValues()) {
                if (item instanceof CutGemItem) {
                    ItemProperties.register(item, cut, (stack, level, entity, seed) ->
                            GemData.isCut(stack) ? 0.1f * (GemData.cut(stack).ordinal() + 1) : 0f);
                }
            }
        });
    }

    /** One line per socket on socketed equipment. */
    private static void tooltip(ItemTooltipEvent e) {
        ItemStack st = e.getItemStack();
        if (!GemData.hasSockets(st)) return;
        List<GemData.Socket> sockets = GemData.sockets(st);
        boolean armor = GemData.isArmor(st);
        List<Component> tip = e.getToolTip();
        int at = Math.min(tip.size(), 1);
        tip.add(at++, Component.translatable("tooltip.ls_lapidary.sockets", sockets.size()).withStyle(ChatFormatting.GRAY));
        for (GemData.Socket s : sockets) {
            if (s.empty()) {
                tip.add(at++, Component.literal(" ◇ ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable("tooltip.ls_lapidary.empty_socket")));
                continue;
            }
            GemEffect ef = s.gem().effect(armor);
            MutableComponent line = Component.literal(" ◆ ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(s.gem().color)));
            line.append(Component.translatable("item.ls_lapidary.cut_gem", GemData.grade(s.quality()), s.cut().displayName(), s.gem().displayName())
                    .withStyle(ChatFormatting.GRAY));
            line.append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(ef.describe(ef.perScale * s.scale()).withStyle(ChatFormatting.BLUE));
            tip.add(at++, line);
        }
    }
}
