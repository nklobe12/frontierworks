package com.lodestar.datastore.client;

import com.lodestar.core.net.CoreNet;
import com.lodestar.datastore.Content;
import com.lodestar.datastore.DatastoreSetup;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class DatastoreClient {
    private DatastoreClient() {}

    public static void init(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            MenuScreens.register(DatastoreSetup.TERMINAL_MENU.get(), TerminalScreen::new);
            MenuScreens.register(DatastoreSetup.CORE_MENU.get(), CoreScreen::new);
            MenuScreens.register(DatastoreSetup.ENCODER_MENU.get(), EncoderScreen::new);
            MenuScreens.register(DatastoreSetup.DRIVE_MENU.get(), DeviceScreen::new);
            MenuScreens.register(DatastoreSetup.FABRICATOR_MENU.get(), DeviceScreen::new);
            MenuScreens.register(DatastoreSetup.PORT_MENU.get(), DeviceScreen::new);
        }));
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerBlockEntityRenderer(Content.DRIVE_BAY_BE.get(), DriveBayRenderer::new));
        CoreNet.registerClient(TerminalMenu.SYNC, TermClient::apply);
        CoreNet.registerClient(TerminalMenu.PLAN, TermClient::plan);
        CoreNet.registerClient(TerminalMenu.JOBS, TermClient::jobs);
    }
}
