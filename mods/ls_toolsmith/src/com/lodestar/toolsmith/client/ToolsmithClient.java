package com.lodestar.toolsmith.client;

import com.lodestar.toolsmith.Content;
import com.lodestar.toolsmith.ModularToolItem;
import com.lodestar.toolsmith.PartItem;
import com.lodestar.toolsmith.ToolData;
import com.lodestar.toolsmith.ToolMaterial;
import com.lodestar.toolsmith.ToolsmithSetup;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** Client setup for Toolsmith: tinting parts and tools by material, broken-tool models, screens and renderers. */
public final class ToolsmithClient {
    private ToolsmithClient() {}

    public static void init(IEventBus bus) {
        ToolsmithSetup.SHIFT = Screen::hasShiftDown;
        bus.addListener(ToolsmithClient::colors);
        bus.addListener(ToolsmithClient::renderers);
        bus.addListener(ToolsmithClient::setup);
    }

    private static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            MenuScreens.register(ToolsmithSetup.FOUNDRY_MENU.get(), FoundryScreen::new);
            MenuScreens.register(ToolsmithSetup.PART_BUILDER_MENU.get(), PartBuilderScreen::new);
            MenuScreens.register(ToolsmithSetup.TOOL_STATION_MENU.get(), ToolStationScreen::new);
            ResourceLocation broken = new ResourceLocation("ls_toolsmith", "broken");
            for (Item it : ToolsmithSetup.toolItems()) {
                ItemProperties.register(it, broken, (stack, level, entity, seed) -> ToolData.isBroken(stack) ? 1f : 0f);
            }
        });
    }

    private static int color(ToolMaterial m) {
        return 0xFF000000 | m.color();
    }

    private static void colors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> color(PartItem.material(stack)), ToolsmithSetup.partItems().toArray(new Item[0]));
        e.register((stack, tint) -> {
            List<ToolMaterial> mats = ToolData.materials(stack);
            if (mats.isEmpty() || !(stack.getItem() instanceof ModularToolItem mt)) return 0xFFFFFFFF;
            // layer 0 handle, layer 1 binding, layer 2 head (parts are stored head, handle, binding)
            int idx = switch (tint) {
                case 0 -> 1;
                case 1 -> 2;
                default -> 0;
            };
            return color(mats.get(Math.min(idx, mats.size() - 1)));
        }, ToolsmithSetup.toolItems().toArray(new Item[0]));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(Content.FOUNDRY_CONTROLLER_BE.get(), FoundryRenderers.Pool::new);
        e.registerBlockEntityRenderer(Content.FOUNDRY_TANK_BE.get(), FoundryRenderers.Tank::new);
        e.registerBlockEntityRenderer(Content.FAUCET_BE.get(), FoundryRenderers.Faucet::new);
        e.registerBlockEntityRenderer(Content.CASTING_TABLE_BE.get(), FoundryRenderers.Casting::new);
        e.registerBlockEntityRenderer(Content.CASTING_BASIN_BE.get(), FoundryRenderers.Casting::new);
    }

    static ItemStack none() {
        return ItemStack.EMPTY;
    }
}
