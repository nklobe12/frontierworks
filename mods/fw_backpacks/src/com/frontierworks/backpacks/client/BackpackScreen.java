package com.frontierworks.backpacks.client;

import com.frontierworks.backpacks.BackpackMenu;
import com.frontierworks.backpacks.Content;
import com.frontierworks.core.client.GuiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 18 + menu.rows * 18 + 14 + 82;
        inventoryLabelY = imageHeight - 93;
    }

    public static void init(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> MenuScreens.register(Content.BACKPACK_MENU.get(), BackpackScreen::new)));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
