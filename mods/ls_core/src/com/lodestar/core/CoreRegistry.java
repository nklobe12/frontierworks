package com.lodestar.core;

import com.lodestar.core.loot.AddItemModifier;
import com.lodestar.core.loot.AddTableModifier;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.registry.LSRegistry;
import com.lodestar.core.util.LS;
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

    private static final LSRegistry REG = Content.REG;

    public static final RegistryObject<MenuType<MachineMenu>> MACHINE_MENU = REG.menu("machine", MachineMenu::new);

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = REG.lootModifiers().register("add_item", AddItemModifier.CODEC);
    public static final RegistryObject<Codec<AddTableModifier>> ADD_TABLE = REG.lootModifiers().register("add_table", AddTableModifier.CODEC);

    public static final RegistryObject<CreativeModeTab> TAB_CORE = tab("core", "ls_codex:codex", new ItemStack(Items.COMPASS), null);
    public static final RegistryObject<CreativeModeTab> TAB_MATERIALS = tab("materials", "ls_veins:tin_ingot", new ItemStack(Items.IRON_INGOT), "core");
    public static final RegistryObject<CreativeModeTab> TAB_INDUSTRY = tab("industry", "ls_machina:pulverizer", new ItemStack(Items.FURNACE), "materials");
    public static final RegistryObject<CreativeModeTab> TAB_POWER = tab("power", "ls_dynamos:stirling_dynamo", new ItemStack(Items.REDSTONE), "industry");
    public static final RegistryObject<CreativeModeTab> TAB_LOGISTICS = tab("logistics", "ls_ducts:item_duct", new ItemStack(Items.HOPPER), "power");
    public static final RegistryObject<CreativeModeTab> TAB_AUTOMATION = tab("automation", "ls_logic:and_gate", new ItemStack(Items.COMPARATOR), "logistics");
    public static final RegistryObject<CreativeModeTab> TAB_TOOLS = tab("tools", "ls_toolsmith:pickaxe", new ItemStack(Items.IRON_PICKAXE), "automation");
    public static final RegistryObject<CreativeModeTab> TAB_GEAR = tab("gear", "ls_jetpacks:jetpack", new ItemStack(Items.ELYTRA), "tools");
    public static final RegistryObject<CreativeModeTab> TAB_ALCHEMY = tab("alchemy", "ls_transmutation:philosophers_stone", new ItemStack(Items.BREWING_STAND), "gear");
    public static final RegistryObject<CreativeModeTab> TAB_EXPLORATION = tab("exploration", "ls_waystones:waystone", new ItemStack(Items.FILLED_MAP), "alchemy");
    public static final RegistryObject<CreativeModeTab> TAB_NATURE = tab("nature", "ls_flora:bluebell", new ItemStack(Items.OAK_SAPLING), "exploration");
    public static final RegistryObject<CreativeModeTab> TAB_CREATURES = tab("creatures", "ls_nightfall:frost_wraith_spawn_egg", new ItemStack(Items.ZOMBIE_HEAD), "nature");
    public static final RegistryObject<CreativeModeTab> TAB_DIMENSIONS = tab("dimensions", "ls_depths:depths_gate", new ItemStack(Items.ENDER_EYE), "creatures");
    public static final RegistryObject<CreativeModeTab> TAB_SPACE = tab("space", "ls_rocketry:rocket", new ItemStack(Items.FIREWORK_ROCKET), "dimensions");
    public static final RegistryObject<CreativeModeTab> TAB_FARMING = tab("farming", "ls_harvest:tomato", new ItemStack(Items.WHEAT), "space");
    public static final RegistryObject<CreativeModeTab> TAB_BUILDING = tab("building", "ls_catwalks:catwalk", new ItemStack(Items.BRICKS), "farming");
    public static final RegistryObject<CreativeModeTab> TAB_TRANSPORT = tab("transport", "ls_airships:airship", new ItemStack(Items.MINECART), "building");

    private static RegistryObject<CreativeModeTab> tab(String name, String iconId, ItemStack fallback, String after) {
        return REG.tabs().register(name, () -> {
            CreativeModeTab.Builder b = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ls_core." + name))
                    .icon(() -> LS.stack(iconId, fallback));
            if (after != null) {
                b.withTabsBefore(new net.minecraft.resources.ResourceLocation("ls_core", after));
            }
            return b.build();
        });
    }

    public static void init() {
        // touching this class performs the static registrations above
    }
}
