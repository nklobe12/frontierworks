package com.frontierworks.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** Shared creative tabs. Every mod adds its items to one of these via FWRegistry. */
public final class FWTab {
    private FWTab() {}

    public static final ResourceKey<CreativeModeTab> MATERIALS = key("materials");
    public static final ResourceKey<CreativeModeTab> MACHINES = key("machines");
    public static final ResourceKey<CreativeModeTab> LOGISTICS = key("logistics");
    public static final ResourceKey<CreativeModeTab> TOOLS = key("tools");
    public static final ResourceKey<CreativeModeTab> ADVENTURE = key("adventure");
    public static final ResourceKey<CreativeModeTab> WORLD = key("world");
    public static final ResourceKey<CreativeModeTab> DECOR = key("decor");
    public static final ResourceKey<CreativeModeTab> SPACE = key("space");

    private static ResourceKey<CreativeModeTab> key(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("fw_core", name));
    }
}
