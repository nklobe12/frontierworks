package com.lodestar.dynamos;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;

import java.util.Map;

/** Fuel values for every dynamo, in FE (items) or FE per mB (fluids). */
public final class DynamoFuels {
    private DynamoFuels() {}

    private static final TagKey<Item> GEMS = ItemTags.create(new ResourceLocation("forge", "gems"));
    private static final TagKey<Fluid> MAGMATIC = FluidTags.create(new ResourceLocation("ls_dynamos", "magmatic_fuel"));

    /** fluid id -> FE per mB for the compression dynamo; fuels from mods built later are listed here too */
    public static final Map<String, Integer> COMPRESSION = Map.of(
            "ls_machina:light_oil", 300, "ls_machina:creosote", 40, "ls_petroleum:diesel", 700, "ls_petroleum:kerosene", 500,
            "ls_petroleum:gasoline", 600, "ls_petroleum:naphtha", 350, "ls_bioenergy:ethanol", 350, "ls_bioenergy:biodiesel", 600,
            "ls_bioenergy:biogas", 150, "ls_fuelcells:hydrogen", 200);

    public static long stirling(ItemStack s) {
        if (s.isEmpty()) return 0;
        return ForgeHooks.getBurnTime(s, RecipeType.SMELTING) * 10L;
    }

    public static long lapidary(ItemStack s) {
        if (s.isEmpty()) return 0;
        if (s.is(Items.DIAMOND)) return 400_000;
        if (s.is(Items.EMERALD)) return 300_000;
        if (s.is(Items.ECHO_SHARD)) return 500_000;
        if (s.is(Items.LAPIS_LAZULI)) return 16_000;
        if (s.is(Items.QUARTZ)) return 24_000;
        if (s.is(Items.AMETHYST_SHARD)) return 12_000;
        if (s.is(Items.PRISMARINE_CRYSTALS)) return 20_000;
        if (s.is(Items.PRISMARINE_SHARD)) return 10_000;
        if (s.is(GEMS)) return 60_000;
        return 0;
    }

    public static long redflux(ItemStack s) {
        if (s.is(Items.REDSTONE)) return 6_000;
        if (s.is(Items.REDSTONE_BLOCK)) return 60_000;
        if (s.is(Items.GLOWSTONE_DUST)) return 8_000;
        if (s.is(Items.GLOWSTONE)) return 34_000;
        return 0;
    }

    public static int magmatic(FluidStack f) {
        if (f.isEmpty()) return 0;
        if (f.getFluid().is(FluidTags.LAVA)) return 100;
        if (f.getFluid().is(MAGMATIC)) return Math.max(50, (f.getFluid().getFluidType().getTemperature() - 300) / 10);
        return 0;
    }

    public static int compression(FluidStack f) {
        if (f.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(f.getFluid());
        return COMPRESSION.getOrDefault(id.toString(), 0);
    }
}
