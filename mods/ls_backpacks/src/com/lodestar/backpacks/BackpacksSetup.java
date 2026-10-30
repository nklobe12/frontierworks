package com.lodestar.backpacks;

import com.lodestar.core.net.CoreNet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public final class BackpacksSetup {
    public static final RegistryObject<MenuType<BackpackMenu>> MENU = Content.REG.menu("backpack", BackpackMenu::new);
    public static final RegistryObject<RecipeSerializer<UpgradeRecipe>> UPGRADE_SERIALIZER = Content.REG.recipeSerializers().register("upgrade", UpgradeRecipe.Serializer::new);

    private BackpacksSetup() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(Upgrades::playerTick);
        MinecraftForge.EVENT_BUS.addListener(Upgrades::pickup);
        CoreNet.register("ls_backpacks:open", (p, d) -> openWorn(p));
        if (com.lodestar.core.test.SelfTest.enabled()) BackpacksTests.register();
    }

    public static Item backpack(BackpackTier t) {
        return switch (t) {
            case LEATHER -> Content.LEATHER_BACKPACK.get();
            case IRON -> Content.IRON_BACKPACK.get();
            case GOLD -> Content.GOLD_BACKPACK.get();
            case DIAMOND -> Content.DIAMOND_BACKPACK.get();
            case NETHERITE -> Content.NETHERITE_BACKPACK.get();
        };
    }

    /** Backpack key: the worn backpack, else the first one in the inventory. */
    static void openWorn(ServerPlayer p) {
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof BackpackItem) {
            BackpackMenu.open(p, chest, BackpackMenu.Source.chest());
            return;
        }
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            ItemStack s = p.getInventory().items.get(i);
            if (s.getItem() instanceof BackpackItem) {
                BackpackMenu.open(p, s, BackpackMenu.Source.inventory(i));
                return;
            }
        }
    }
}
