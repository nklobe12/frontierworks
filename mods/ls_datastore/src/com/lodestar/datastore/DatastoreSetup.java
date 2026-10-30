package com.lodestar.datastore;

import com.lodestar.core.net.CoreNet;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import com.lodestar.datastore.menu.CoreMenu;
import com.lodestar.datastore.menu.DeviceMenu;
import com.lodestar.datastore.menu.EncoderMenu;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Menus, packets and recipe-viewer pages for Datastore. */
public final class DatastoreSetup {
    private DatastoreSetup() {}

    public static final RegistryObject<MenuType<TerminalMenu>> TERMINAL_MENU = Content.REG.menu("terminal", TerminalMenu::new);
    public static final RegistryObject<MenuType<CoreMenu>> CORE_MENU = Content.REG.menu("core", CoreMenu::new);
    public static final RegistryObject<MenuType<EncoderMenu>> ENCODER_MENU = Content.REG.menu("pattern_encoder", EncoderMenu::new);
    public static final RegistryObject<MenuType<DeviceMenu>> DRIVE_MENU = Content.REG.menu("drive_bay",
            (id, inv, buf) -> DeviceMenu.client(DatastoreSetup.DRIVE_MENU.get(), DeviceMenu.Kind.DRIVE, id, inv, buf));
    public static final RegistryObject<MenuType<DeviceMenu>> FABRICATOR_MENU = Content.REG.menu("fabricator",
            (id, inv, buf) -> DeviceMenu.client(DatastoreSetup.FABRICATOR_MENU.get(), DeviceMenu.Kind.FABRICATOR, id, inv, buf));
    public static final RegistryObject<MenuType<DeviceMenu>> PORT_MENU = Content.REG.menu("port",
            (id, inv, buf) -> DeviceMenu.client(DatastoreSetup.PORT_MENU.get(), DeviceMenu.Kind.PORT, id, inv, buf));

    public static void init(IEventBus bus) {
        CoreNet.registerTag(TerminalMenu.ACTION, (sp, tag) -> {
            if (sp.containerMenu instanceof TerminalMenu m && m.containerId == tag.getInt("W")) m.handle(sp, tag);
        });
        lens();
        if (com.lodestar.core.test.SelfTest.enabled()) DatastoreTests.register();
    }

    private static void lens() {
        ResourceLocation crafting = new ResourceLocation("minecraft", "crafting");
        LensRegistry.addCatalyst(crafting, () -> new ItemStack(Content.FABRICATOR.get()));
        LensRegistry.addCatalyst(crafting, () -> new ItemStack(Content.CRAFTING_TERMINAL.get()));
        LensRegistry.addCatalyst(crafting, () -> new ItemStack(Content.PATTERN_ENCODER.get()));
        ResourceLocation disks = new ResourceLocation("ls_datastore", "disks");
        LensRegistry.register(new RecipeCategory(disks, Component.translatable("lens.category.ls_datastore.disks"),
                () -> new ItemStack(Content.DRIVE_BAY.get()), () -> List.of(new ItemStack(Content.DRIVE_BAY.get())), 260), level -> {
            List<RecipeView> out = new ArrayList<>();
            Item[] tiers = {Content.COPPER_DATA_DISK.get(), Content.ELECTRUM_DATA_DISK.get(), Content.PLATINUM_DATA_DISK.get(), Content.COBALT_DATA_DISK.get()};
            for (Item it : tiers) {
                DiskItem d = (DiskItem) it;
                out.add(RecipeView.builder(disks, 150, 26).catalyst(4, 4, List.of(new ItemStack(it)), Component.translatable("lens.ls_core.not_consumed"))
                        .text(26, 3, Component.translatable("lens.ls_datastore.disk_info", String.format("%,d", d.bytes), d.types))
                        .text(26, 14, Component.translatable("lens.ls_datastore.disk_cost", d.bytesPerType()), 0xFF606060).build());
            }
            return out;
        });
    }
}
