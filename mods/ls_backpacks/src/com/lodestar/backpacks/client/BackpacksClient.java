package com.lodestar.backpacks.client;

import com.lodestar.backpacks.BackpackBE;
import com.lodestar.backpacks.BackpackItem;
import com.lodestar.backpacks.BackpackTier;
import com.lodestar.backpacks.BackpacksSetup;
import com.lodestar.backpacks.Content;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.core.net.CoreNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class BackpacksClient {
    public static final KeyMapping OPEN = new KeyMapping("key.ls_backpacks.open", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.inventory");

    private BackpacksClient() {}

    public static void init(IEventBus bus) {
        for (BackpackTier t : BackpackTier.values()) RenderUtil.extraModel(new ResourceLocation("ls_backpacks", "block/worn_" + t.id()));
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> MenuScreens.register(BackpacksSetup.MENU.get(), BackpackScreen::new)));
        bus.addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN));
        bus.addListener((RegisterColorHandlersEvent.Item e) -> {
            for (BackpackTier t : BackpackTier.values()) {
                e.register((stack, layer) -> layer == 0 && stack.getItem() instanceof BackpackItem bi ? bi.getColor(stack) : -1, BackpacksSetup.backpack(t));
            }
        });
        bus.addListener((RegisterColorHandlersEvent.Block e) -> e.register((state, level, pos, tint) -> {
            if (tint != 0 || level == null || pos == null) return -1;
            return level.getBlockEntity(pos) instanceof BackpackBE be && be.color() != -1 ? be.color() : -1;
        }, Content.PLACED_BACKPACK.get()));
        bus.addListener(BackpacksClient::layers);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END) return;
            while (OPEN.consumeClick()) {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) CoreNet.send("ls_backpacks:open", 0);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void layers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer pr) pr.addLayer(new BackpackLayer((net.minecraft.client.renderer.entity.RenderLayerParent<AbstractClientPlayer, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>>) pr));
        }
    }
}
