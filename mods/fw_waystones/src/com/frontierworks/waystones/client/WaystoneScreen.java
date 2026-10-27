package com.frontierworks.waystones.client;

import com.frontierworks.core.client.GuiDraw;
import com.frontierworks.waystones.Content;
import com.frontierworks.waystones.WaystoneData;
import com.frontierworks.waystones.WaystoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Paged list of activated waystones; click one to travel there. */
public class WaystoneScreen extends AbstractContainerScreen<WaystoneMenu> {
    private static final int PER_PAGE = 7;
    private int page;

    public WaystoneScreen(WaystoneMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 200;
        imageHeight = 200;
    }

    public static void init(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> MenuScreens.register(Content.WAYSTONE_MENU.get(), WaystoneScreen::new)));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(menu.entries.size(), start + PER_PAGE); i++) {
            final int idx = i;
            WaystoneData.Entry e = menu.entries.get(i);
            boolean here = e.pos.equals(menu.origin);
            int cost = minecraft != null && minecraft.player != null ? WaystoneMenu.cost(minecraft.player, menu.origin, e) : 0;
            String label = e.name + (here ? "  (here)" : cost > 0 ? "  [" + cost + " lvl]" : "");
            Button b = Button.builder(Component.literal(label), btn -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, idx);
            }).bounds(leftPos + 10, topPos + 22 + (i - start) * 22, 180, 20).build();
            b.active = !here;
            addRenderableWidget(b);
        }
        int pages = Math.max(1, (menu.entries.size() + PER_PAGE - 1) / PER_PAGE);
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                page = (page + pages - 1) % pages;
                rebuild();
            }).bounds(leftPos + 10, topPos + 176, 20, 18).build());
            addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                page = (page + 1) % pages;
                rebuild();
            }).bounds(leftPos + 170, topPos + 176, 20, 18).build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 8, GuiDraw.TEXT, false);
        if (menu.entries.size() <= 1) {
            g.drawString(font, Component.translatable("gui.fw_waystones.empty"), 12, 30, GuiDraw.TEXT, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
    }
}
