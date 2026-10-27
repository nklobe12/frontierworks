package com.lodestar.transmutation.client;

import com.lodestar.core.net.CoreNet;
import com.lodestar.transmutation.AetherValues;
import com.lodestar.transmutation.MatterTools;
import com.lodestar.transmutation.TransmutationSetup;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class TransmutationClient {
    public static final KeyMapping CHARGE = new KeyMapping("key.ls_transmutation.charge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.lodestar");

    private TransmutationClient() {}

    public static void init(IEventBus bus) {
        CoreNet.registerClient("ls_transmutation:values", AetherValues::receive);
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> MenuScreens.register(TransmutationSetup.EXCHANGE_MENU.get(), ExchangeScreen::new)));
        bus.addListener((RegisterKeyMappingsEvent e) -> e.register(CHARGE));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END) return;
            while (CHARGE.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof MatterTools.Chargeable) CoreNet.send("ls_transmutation:charge", 0);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> {
            long v = AetherValues.get(e.getItemStack());
            if (v > 0) {
                String s = AetherValues.format(v) + (e.getItemStack().getCount() > 1 ? " (" + AetherValues.format(v * e.getItemStack().getCount()) + ")" : "");
                e.getToolTip().add(Component.translatable("tooltip.ls_transmutation.value", s).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }
}
