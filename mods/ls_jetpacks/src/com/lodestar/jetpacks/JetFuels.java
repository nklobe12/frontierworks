package com.lodestar.jetpacks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What fuel jetpacks can burn and how good each fuel is (its grade divides consumption). Fuels from other mods are
 * picked up through common fluid tags (forge:diesel, forge:kerosene, ...), so later fuels work without changes here.
 */
public final class JetFuels {
    private JetFuels() {}

    private static final Map<ResourceLocation, Double> IDS = new LinkedHashMap<>();
    private static final Map<TagKey<Fluid>, Double> TAGS = new LinkedHashMap<>();

    static {
        IDS.put(new ResourceLocation("ls_machina", "creosote"), 1.0);
        IDS.put(new ResourceLocation("ls_machina", "light_oil"), 2.0);
        tag("crude_oil", 0.75);
        tag("oil", 0.75);
        tag("creosote", 1.0);
        tag("light_oil", 2.0);
        tag("ethanol", 2.0);
        tag("biodiesel", 3.0);
        tag("diesel", 3.0);
        tag("kerosene", 4.0);
        tag("jet_fuel", 5.0);
        tag("rocket_fuel", 6.0);
    }

    private static void tag(String path, double grade) {
        TAGS.put(FluidTags.create(new ResourceLocation("forge", path)), grade);
    }

    /** Fuel grade (0 = not a fuel). */
    public static double grade(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        Double g = id == null ? null : IDS.get(id);
        double best = g == null ? 0 : g;
        for (Map.Entry<TagKey<Fluid>, Double> e : TAGS.entrySet()) {
            if (fluid.is(e.getKey())) best = Math.max(best, e.getValue());
        }
        return best;
    }

    public static double grade(FluidStack fs) {
        return fs.isEmpty() ? 0 : grade(fs.getFluid());
    }

    /** Every registered fluid that is a fuel (source fluids only), for Lens. */
    public static List<Fluid> all() {
        return ForgeRegistries.FLUIDS.getValues().stream().filter(f -> f.isSource(f.defaultFluidState()) && grade(f) > 0).toList();
    }
}
