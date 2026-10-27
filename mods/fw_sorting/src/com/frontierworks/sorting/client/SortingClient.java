package com.frontierworks.sorting.client;

import com.frontierworks.core.net.CoreNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/** Press R (configurable) in an inventory screen: sorts the container under the cursor (or your inventory). */
public final class SortingClient {
    public static final KeyMapping SORT = new KeyMapping("key.fw_sorting.sort", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.frontierworks");

    private SortingClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent e) -> e.register(SORT));
        MinecraftForge.EVENT_BUS.addListener(SortingClient::onKey);
    }

    private static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen) || !SORT.matches(event.getKeyCode(), event.getScanCode())) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        Slot hovered = screen.getSlotUnderMouse();
        boolean playerInv = screen instanceof InventoryScreen || (hovered != null && hovered.container instanceof Inventory);
        CoreNet.send("fw_sorting:sort", playerInv ? 1 : 0);
        event.setCanceled(true);
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
