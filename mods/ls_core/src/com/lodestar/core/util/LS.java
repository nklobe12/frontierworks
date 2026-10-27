package com.lodestar.core.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Cross-mod lookups by id so mods can reference each other without hard class dependencies. */
public final class LS {
    private LS() {}

    public static Item item(String id) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? Items.AIR : it;
    }

    public static ItemStack stack(String id, int count) {
        Item it = item(id);
        return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it, count);
    }

    public static ItemStack stack(String id, ItemStack fallback) {
        Item it = item(id);
        return it == Items.AIR ? fallback.copy() : new ItemStack(it);
    }

    public static Block block(String id) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return b == null ? Blocks.AIR : b;
    }

    public static Fluid fluid(String id) {
        Fluid f = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(id));
        return f == null ? Fluids.EMPTY : f;
    }

    public static boolean loaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static boolean isFW(ResourceLocation id) {
        return id != null && id.getNamespace().startsWith("ls_");
    }
}
