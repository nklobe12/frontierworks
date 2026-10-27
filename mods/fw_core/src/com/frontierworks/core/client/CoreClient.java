package com.frontierworks.core.client;

import com.frontierworks.core.CoreRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class CoreClient {
    private static final Logger LOG = LogUtils.getLogger();
    private static int quitTimer = -1;

    private CoreClient() {}

    public static void init(IEventBus bus) {
        bus.addListener(CoreClient::setup);
        MinecraftForge.EVENT_BUS.addListener(CoreClient::onTooltip);
        if ("client".equals(System.getProperty("fwcore.selftest"))) {
            MinecraftForge.EVENT_BUS.addListener(CoreClient::onScreen);
            MinecraftForge.EVENT_BUS.addListener(CoreClient::onClientTick);
        }
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(CoreRegistry.MACHINE_MENU.get(), MachineScreen::new));
    }

    /** Adds a gray description line for any Frontierworks item whose lang file has "<key>.desc". */
    private static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !id.getNamespace().startsWith("fw_")) return;
        String key = event.getItemStack().getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            for (String line : I18n.get(key).split("\n")) {
                event.getToolTip().add(1, Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static void onScreen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && quitTimer < 0) {
            LOG.info("FWSELFTEST CLIENT reached title screen");
            quitTimer = 60;
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || quitTimer < 0) return;
        if (--quitTimer == 0) {
            LOG.info("FWSELFTEST CLIENT DONE");
            Minecraft.getInstance().stop();
        }
    }
}
