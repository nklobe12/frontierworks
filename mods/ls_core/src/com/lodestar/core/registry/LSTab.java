package com.lodestar.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** Shared creative tabs, one per domain of the pack (see tools/plan.py TABS). Every mod adds its items to one of these. */
public final class LSTab {
    private LSTab() {}

    public static final ResourceKey<CreativeModeTab> CORE = key("core");
    public static final ResourceKey<CreativeModeTab> MATERIALS = key("materials");
    public static final ResourceKey<CreativeModeTab> INDUSTRY = key("industry");
    public static final ResourceKey<CreativeModeTab> POWER = key("power");
    public static final ResourceKey<CreativeModeTab> LOGISTICS = key("logistics");
    public static final ResourceKey<CreativeModeTab> AUTOMATION = key("automation");
    public static final ResourceKey<CreativeModeTab> TOOLS = key("tools");
    public static final ResourceKey<CreativeModeTab> GEAR = key("gear");
    public static final ResourceKey<CreativeModeTab> ALCHEMY = key("alchemy");
    public static final ResourceKey<CreativeModeTab> EXPLORATION = key("exploration");
    public static final ResourceKey<CreativeModeTab> NATURE = key("nature");
    public static final ResourceKey<CreativeModeTab> CREATURES = key("creatures");
    public static final ResourceKey<CreativeModeTab> DIMENSIONS = key("dimensions");
    public static final ResourceKey<CreativeModeTab> SPACE = key("space");
    public static final ResourceKey<CreativeModeTab> FARMING = key("farming");
    public static final ResourceKey<CreativeModeTab> BUILDING = key("building");
    public static final ResourceKey<CreativeModeTab> TRANSPORT = key("transport");

    private static ResourceKey<CreativeModeTab> key(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("ls_core", name));
    }
}
