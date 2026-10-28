package com.lodestar.codex.client;

import com.lodestar.codex.CodexItem;
import com.lodestar.codex.Content;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/**
 * Client setup for the Codex: opening it from the item, the look-up key (hold it over an item in any inventory, or
 * press it while looking at a block), tracking which items the player has carried, and saving reading progress.
 */
public final class CodexClient {
    public static final KeyMapping LOOKUP = new KeyMapping("key.ls_codex.lookup", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.lodestar");
    /** How long the key must be held over an item before the Codex opens. */
    private static final long HOLD_MS = 650;

    private static Item holdItem;
    private static long holdStart;
    private static int ticks;

    private CodexClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent e) -> e.register(LOOKUP));
        bus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) rm -> CodexBook.invalidate()));
        CodexItem.openClient = () -> CodexScreen.open(null);
        MinecraftForge.EVENT_BUS.addListener(CodexClient::onTick);
        MinecraftForge.EVENT_BUS.addListener(CodexClient::onScreenRender);
        MinecraftForge.EVENT_BUS.addListener(CodexClient::onTooltip);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> CodexProgress.unload());
    }

    private static boolean keyDown() {
        Minecraft mc = Minecraft.getInstance();
        InputConstants.Key key = LOOKUP.getKey();
        if (key.getValue() == InputConstants.UNKNOWN.getValue()) return false;
        long win = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(win, key.getValue()) == GLFW.GLFW_PRESS;
        return InputConstants.isKeyDown(win, key.getValue());
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (++ticks % 20 == 0) CodexProgress.scan(mc.player);
        if (ticks % 1200 == 0) CodexProgress.save();
        while (LOOKUP.consumeClick()) {
            if (mc.screen != null) continue;
            boolean has = mc.player.isCreative() || mc.player.getInventory().contains(new net.minecraft.world.item.ItemStack(Content.FIELD_CODEX.get()));
            if (!has) {
                mc.player.displayClientMessage(Component.translatable("msg.ls_codex.need_codex").withStyle(ChatFormatting.GRAY), true);
                continue;
            }
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK && mc.level != null) {
                Item item = mc.level.getBlockState(bh.getBlockPos()).getBlock().asItem();
                if (CodexScreen.openFor(item, null)) continue;
            }
            CodexScreen.open(null);
        }
    }

    /** Hold-to-look-up: a ring fills around the cursor while the key is held over an item that has a page. */
    private static void onScreenRender(ScreenEvent.Render.Post e) {
        Screen screen = e.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> acs) || (screen.getFocused() instanceof EditBox eb && eb.isFocused())) {
            holdItem = null;
            return;
        }
        Slot slot = acs.getSlotUnderMouse();
        if (slot == null || !slot.hasItem() || !keyDown()) {
            holdItem = null;
            return;
        }
        Item item = slot.getItem().getItem();
        if (CodexBook.find(item) == null) {
            holdItem = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (item != holdItem) {
            holdItem = item;
            holdStart = now;
        }
        float f = Math.min(1f, (now - holdStart) / (float) HOLD_MS);
        ring(e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), f);
        if (f >= 1f) {
            holdItem = null;
            Minecraft.getInstance().tell(() -> CodexScreen.openFor(item, screen));
        }
    }

    private static void ring(GuiGraphics g, int mx, int my, float f) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / steps;
            int x = mx + (int) Math.round(Math.cos(a) * 9), y = my + (int) Math.round(Math.sin(a) * 9);
            boolean on = i < Math.round(f * steps);
            g.fill(x - 1, y - 1, x + 1, y + 1, on ? 0xFFE8C25A : 0x60000000);
        }
        g.pose().popPose();
    }

    private static void onTooltip(ItemTooltipEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?>) || e.getEntity() == null) return;
        if (CodexBook.find(e.getItemStack().getItem()) == null) return;
        e.getToolTip().add(Component.translatable("gui.ls_codex.hold_lookup", LOOKUP.getTranslatedKeyMessage().getString()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
