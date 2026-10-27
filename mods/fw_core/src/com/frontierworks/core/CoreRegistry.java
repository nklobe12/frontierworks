package com.frontierworks.core;

import com.frontierworks.core.loot.AddItemModifier;
import com.frontierworks.core.loot.AddTableModifier;
import com.frontierworks.core.machine.MachineMenu;
import com.frontierworks.core.registry.FWRegistry;
import com.frontierworks.core.util.FW;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;

/** Hand-written core registrations (generated items live in {@link Content}). */
public final class CoreRegistry {
    private CoreRegistry() {}

    private static final FWRegistry REG = Content.REG;

    public static final RegistryObject<MenuType<MachineMenu>> MACHINE_MENU = REG.menu("machine", MachineMenu::new);

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = REG.lootModifiers().register("add_item", AddItemModifier.CODEC);
    public static final RegistryObject<Codec<AddTableModifier>> ADD_TABLE = REG.lootModifiers().register("add_table", AddTableModifier.CODEC);

    public static final RegistryObject<CreativeModeTab> TAB_MATERIALS = tab("materials", "fw_metals:tin_ingot", new ItemStack(Items.IRON_INGOT), null);
    public static final RegistryObject<CreativeModeTab> TAB_MACHINES = tab("machines", "fw_machines:pulverizer", new ItemStack(Items.FURNACE), "materials");
    public static final RegistryObject<CreativeModeTab> TAB_LOGISTICS = tab("logistics", "fw_pipes:item_pipe", new ItemStack(Items.HOPPER), "machines");
    public static final RegistryObject<CreativeModeTab> TAB_TOOLS = tab("tools", "fw_jetpack:jetpack_basic", new ItemStack(Items.IRON_PICKAXE), "logistics");
    public static final RegistryObject<CreativeModeTab> TAB_ADVENTURE = tab("adventure", "fw_relics:amulet_of_vigor", new ItemStack(Items.IRON_SWORD), "tools");
    public static final RegistryObject<CreativeModeTab> TAB_WORLD = tab("world", "fw_rubber:rubber_sapling", new ItemStack(Items.GRASS_BLOCK), "adventure");
    public static final RegistryObject<CreativeModeTab> TAB_SPACE = tab("space", "fw_rocketry:rocket_t1", new ItemStack(Items.FIREWORK_ROCKET), "world");
    public static final RegistryObject<CreativeModeTab> TAB_DECOR = tab("decor", "fw_lamps:lamp_white", new ItemStack(Items.REDSTONE_LAMP), "space");

    private static RegistryObject<CreativeModeTab> tab(String name, String iconId, ItemStack fallback, String after) {
        return REG.tabs().register(name, () -> {
            CreativeModeTab.Builder b = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fw_core." + name))
                    .icon(() -> FW.stack(iconId, fallback));
            if (after != null) {
                b.withTabsBefore(new net.minecraft.resources.ResourceLocation("fw_core", after));
            }
            return b.build();
        });
    }

    public static void init() {
        // touching this class performs the static registrations above
    }
}
