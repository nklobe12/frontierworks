package com.lodestar.toolsmith.foundry;

import com.lodestar.toolsmith.CastItem;
import com.lodestar.toolsmith.Materials;
import com.lodestar.toolsmith.PartItem;
import com.lodestar.toolsmith.PartType;
import com.lodestar.toolsmith.ToolMaterial;
import com.lodestar.toolsmith.ToolsmithSetup;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** What melts into what in the foundry, how molten metals alloy, and what casting tables and basins make. */
public final class FoundryRules {
    public static final int INGOT = 144, NUGGET = 16, BLOCK = 1296;
    /** shape -> (forge tag folder, mB) */
    public static final Map<String, Integer> SHAPES = new java.util.LinkedHashMap<>();
    private static final String[][] FORMS = {{"ingots", "144"}, {"nuggets", "16"}, {"storage_blocks", "1296"}, {"dusts", "144"},
            {"plates", "144"}, {"gears", "576"}, {"rods", "72"}, {"wires", "48"}, {"ores", "288"}, {"raw_materials", "216"}};

    static {
        SHAPES.put("ingot", 144);
        SHAPES.put("nugget", 16);
        SHAPES.put("plate", 144);
        SHAPES.put("gear", 576);
        SHAPES.put("rod", 72);
        for (PartType p : PartType.values()) SHAPES.put(p.id, p.cost * INGOT);
    }

    public record Alloy(List<FluidStack> inputs, FluidStack output) {}

    private static final Map<Item, FluidStack> MELT_CACHE = new IdentityHashMap<>();
    private static final FluidStack NONE = FluidStack.EMPTY;
    private static List<Alloy> alloys;

    private FoundryRules() {}

    public static void clearCaches() {
        MELT_CACHE.clear();
    }

    public static String metalOf(Fluid f) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(f);
        return id.getNamespace().equals("ls_toolsmith") && id.getPath().startsWith("molten_") ? id.getPath().substring(7) : "";
    }

    public static Fluid molten(String metal) {
        Fluid f = BuiltInRegistries.FLUID.get(new ResourceLocation("ls_toolsmith", "molten_" + metal));
        return f == null ? Fluids.EMPTY : f;
    }

    /** Molten result of melting one of this item, or empty. */
    public static FluidStack melting(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        if (stack.getItem() instanceof PartItem pi) {
            ToolMaterial m = PartItem.material(stack);
            if (!m.castable()) return FluidStack.EMPTY;
            Fluid f = BuiltInRegistries.FLUID.get(new ResourceLocation(m.fluid()));
            return f == null || f == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(f, pi.part.cost * INGOT);
        }
        if (stack.getItem() instanceof CastItem) return new FluidStack(molten("gold"), 288);
        FluidStack cached = MELT_CACHE.get(stack.getItem());
        if (cached == null) {
            cached = computeMelting(stack);
            MELT_CACHE.put(stack.getItem(), cached);
        }
        return cached.isEmpty() ? FluidStack.EMPTY : cached.copy();
    }

    private static FluidStack computeMelting(ItemStack stack) {
        if (stack.is(Items.NETHERITE_INGOT)) return new FluidStack(molten("netherite"), INGOT);
        if (stack.is(Items.NETHERITE_SCRAP)) return new FluidStack(molten("netherite"), 36);
        for (String metal : ToolsmithSetup.MOLTEN_METALS) {
            Fluid f = molten(metal);
            if (f == Fluids.EMPTY) continue;
            for (String[] form : FORMS) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation("forge", form[0] + "/" + metal));
                if (stack.is(tag)) return new FluidStack(f, Integer.parseInt(form[1]));
            }
        }
        return NONE;
    }

    /** Melting temperature in C (hotter metals melt slower). */
    public static int temperature(FluidStack f) {
        return Math.max(300, f.getFluid().getFluidType().getTemperature() - 273);
    }

    public static List<Alloy> alloys() {
        if (alloys == null) {
            alloys = new ArrayList<>();
            for (Object[] a : ToolsmithSetup.ALLOYS) {
                List<FluidStack> in = new ArrayList<>();
                @SuppressWarnings("unchecked") Map<String, Integer> ins = (Map<String, Integer>) a[1];
                for (Map.Entry<String, Integer> e : ins.entrySet()) in.add(new FluidStack(molten(e.getKey()), e.getValue()));
                alloys.add(new Alloy(in, new FluidStack(molten((String) a[0]), (Integer) a[2])));
            }
        }
        return alloys;
    }

    // ------------------------------------------------------------------ casting

    public record CastResult(ItemStack output, int amount, boolean consumesCast, int coolTicks) {}

    /** Shape of an item placed on a table to make a gold cast from, or null. */
    @Nullable
    public static String shapeOf(ItemStack stack) {
        if (stack.getItem() instanceof PartItem pi) return pi.part.id;
        String[][] map = {{"ingots", "ingot"}, {"nuggets", "nugget"}, {"plates", "plate"}, {"gears", "gear"}, {"rods", "rod"}};
        for (String[] m : map) {
            if (stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", m[0])))) return m[1];
        }
        return null;
    }

    /** Result of pouring this fluid onto a casting table holding this item (cast or example item), or null. */
    @Nullable
    public static CastResult table(ItemStack held, Fluid fluid) {
        if (held.getItem() instanceof CastItem cast) {
            String metal = metalOf(fluid);
            if (metal.isEmpty()) return null;
            PartType part = PartType.byId(cast.shape);
            if (part != null) {
                ToolMaterial m = Materials.byFluid(BuiltInRegistries.FLUID.getKey(fluid).toString());
                if (m == null) return null;
                return new CastResult(PartItem.of(part, m), part.cost * INGOT, false, 40 + part.cost * 10);
            }
            String folder = switch (cast.shape) {
                case "ingot" -> "ingots";
                case "nugget" -> "nuggets";
                case "plate" -> "plates";
                case "gear" -> "gears";
                case "rod" -> "rods";
                default -> null;
            };
            if (folder == null) return null;
            ItemStack out = firstOfTag("forge", folder + "/" + metal);
            if (out.isEmpty()) return null;
            return new CastResult(out, SHAPES.get(cast.shape), false, cast.shape.equals("nugget") ? 10 : 40);
        }
        String shape = shapeOf(held);
        if (shape != null && (fluid.isSame(molten("gold")) || fluid.isSame(molten("brass")))) {
            Item castItem = ToolsmithSetup.castItem(shape);
            if (castItem == null) return null;
            return new CastResult(new ItemStack(castItem), 288, true, 80);
        }
        return null;
    }

    /** Result of filling a casting basin with this fluid, or null. */
    @Nullable
    public static CastResult basin(Fluid fluid) {
        String metal = metalOf(fluid);
        if (metal.isEmpty()) return null;
        if (metal.equals("netherite")) return new CastResult(new ItemStack(Items.NETHERITE_BLOCK), BLOCK, false, 200);
        ItemStack out = firstOfTag("forge", "storage_blocks/" + metal);
        return out.isEmpty() ? null : new CastResult(out, BLOCK, false, 160);
    }

    private static final Map<String, ItemStack> TAG_FIRST = new HashMap<>();

    /** First item of a tag, preferring vanilla, then Lodestar materials mods. */
    public static ItemStack firstOfTag(String ns, String path) {
        String key = ns + ":" + path;
        ItemStack cached = TAG_FIRST.get(key);
        if (cached != null) return cached.copy();
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(ns, path));
        Item best = null;
        int bestRank = 99;
        for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            String n = BuiltInRegistries.ITEM.getKey(h.value()).getNamespace();
            int rank = n.equals("minecraft") ? 0 : n.equals("ls_veins") ? 1 : n.equals("ls_metalwork") ? 2 : 5;
            if (rank < bestRank) {
                best = h.value();
                bestRank = rank;
            }
        }
        ItemStack out = best == null ? ItemStack.EMPTY : new ItemStack(best);
        if (best != null) TAG_FIRST.put(key, out.copy());
        return out;
    }

    public static void clearTagCache() {
        TAG_FIRST.clear();
        MELT_CACHE.clear();
    }
}
