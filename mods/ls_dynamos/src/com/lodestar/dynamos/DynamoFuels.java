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

    /**
     * Common forge fluid tag -> FE per mB for the compression dynamo. Any mod's fuel in one of these tags burns,
     * the id map above only adds exact overrides.
     */
    public static final Map<String, Integer> COMPRESSION_TAGS = Map.ofEntries(
            Map.entry("forge:creosote", 40), Map.entry("forge:light_oil", 300), Map.entry("forge:crude_oil", 150),
            Map.entry("forge:heavy_oil", 250), Map.entry("forge:naphtha", 350), Map.entry("forge:kerosene", 500),
            Map.entry("forge:gasoline", 600), Map.entry("forge:diesel", 700), Map.entry("forge:biodiesel", 600),
            Map.entry("forge:ethanol", 350), Map.entry("forge:biogas", 150), Map.entry("forge:refinery_gas", 120),
            Map.entry("forge:hydrogen", 200));

    public static int compression(FluidStack f) {
        if (f.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(f.getFluid());
        Integer exact = COMPRESSION.get(id.toString());
        if (exact != null) return exact;
        int best = 0;
        for (Map.Entry<String, Integer> e : COMPRESSION_TAGS.entrySet()) {
            if (e.getValue() > best && f.getFluid().is(FluidTags.create(new ResourceLocation(e.getKey())))) best = e.getValue();
        }
        return best;
    }
}
