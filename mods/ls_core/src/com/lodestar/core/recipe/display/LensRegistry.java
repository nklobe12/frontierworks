package com.lodestar.core.recipe.display;

import com.lodestar.core.recipe.ChanceStack;
import com.lodestar.core.recipe.CountedIngredient;
import com.lodestar.core.recipe.MachineRecipe;
import com.lodestar.core.recipe.MachineRecipeKind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Where mods tell the Lens recipe viewer (and Codex) what recipes exist. Each category has a provider that
 * turns the current recipes (or any other data) into {@link RecipeView}s. Machine recipe kinds get a generic
 * layout automatically through {@link #machine}. Safe to call from common setup.
 */
public final class LensRegistry {
    public interface Provider {
        List<RecipeView> views(Level level);
    }

    private static final Map<ResourceLocation, RecipeCategory> CATEGORIES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Provider> PROVIDERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<Supplier<ItemStack>>> EXTRA_CATALYSTS = new LinkedHashMap<>();

    private LensRegistry() {}

    public static synchronized void register(RecipeCategory category, Provider provider) {
        CATEGORIES.put(category.id, category);
        PROVIDERS.put(category.id, provider);
    }

    /** Adds a block or item that also performs recipes of a category (e.g. an electric furnace for smelting). */
    public static synchronized void addCatalyst(ResourceLocation category, Supplier<ItemStack> stack) {
        EXTRA_CATALYSTS.computeIfAbsent(category, k -> new ArrayList<>()).add(stack);
    }

    public static synchronized List<ItemStack> extraCatalysts(ResourceLocation category) {
        List<ItemStack> out = new ArrayList<>();
        for (Supplier<ItemStack> s : EXTRA_CATALYSTS.getOrDefault(category, List.of())) out.add(s.get());
        return out;
    }

    public static synchronized List<RecipeCategory> categories() {
        List<RecipeCategory> list = new ArrayList<>(CATEGORIES.values());
        list.sort((a, b) -> Integer.compare(a.order, b.order));
        return list;
    }

    public static synchronized Provider provider(ResourceLocation category) {
        return PROVIDERS.get(category);
    }

    /** Registers a machine recipe kind with the generic machine layout. */
    public static void machine(MachineRecipeKind kind, Supplier<ItemStack> machine, int order) {
        ResourceLocation id = kind.id;
        RecipeCategory cat = new RecipeCategory(id, Component.translatable("lens.category." + id.getNamespace() + "." + id.getPath()),
                machine, () -> Collections.singletonList(machine.get()), order);
        register(cat, level -> {
            List<RecipeView> out = new ArrayList<>();
            for (MachineRecipe r : kind.all(level)) out.add(machineView(id, r));
            return out;
        });
    }

    /** Generic layout: fluid inputs, item inputs (two columns), arrow, item outputs, fluid outputs, energy and time below. */
    public static RecipeView machineView(ResourceLocation category, MachineRecipe r) {
        int inCols = r.inputs.size() > 2 ? 2 : Math.max(1, r.inputs.size());
        int outCols = r.outputs.size() > 2 ? 2 : Math.max(1, r.outputs.size());
        int tanksIn = (r.fluidIn.isEmpty() ? 0 : 1) + (r.fluidIn2.isEmpty() ? 0 : 1);
        int tanksOut = (r.fluidOut.isEmpty() ? 0 : 1) + (r.fluidOut2.isEmpty() ? 0 : 1);
        int width = Math.max(120, tanksIn * 20 + inCols * 18 + 30 + outCols * 18 + tanksOut * 20 + 8);
        RecipeView.Builder b = RecipeView.builder(category, width, 62).id(r.getId());
        int x = 2;
        if (!r.fluidIn.isEmpty()) {
            b.tank(x, 2, 16, 40, r.fluidIn, Math.max(1000, r.fluidIn.getAmount()), false);
            x += 20;
        }
        if (!r.fluidIn2.isEmpty()) {
            b.tank(x, 2, 16, 40, r.fluidIn2, Math.max(1000, r.fluidIn2.getAmount()), false);
            x += 20;
        }
        int rows = (r.inputs.size() + inCols - 1) / inCols;
        int y0 = rows > 1 ? 4 : 13;
        for (int i = 0; i < r.inputs.size(); i++) {
            CountedIngredient ci = r.inputs.get(i);
            List<ItemStack> st = new ArrayList<>();
            for (ItemStack s : ci.ingredient.getItems()) {
                ItemStack c = s.copy();
                c.setCount(Math.max(1, ci.count));
                st.add(c);
            }
            if (ci.isCatalyst()) {
                b.catalyst(x + (i % inCols) * 18, y0 + (i / inCols) * 18, st, Component.translatable("lens.ls_core.not_consumed"));
            } else {
                b.input(x + (i % inCols) * 18, y0 + (i / inCols) * 18, st);
            }
        }
        x += inCols * 18 + 4;
        b.arrow(x, 13);
        x += 28;
        int orows = (r.outputs.size() + outCols - 1) / outCols;
        int oy0 = orows > 1 ? 4 : 13;
        for (int i = 0; i < r.outputs.size(); i++) {
            ChanceStack cs = r.outputs.get(i);
            b.output(x + (i % outCols) * 18, oy0 + (i / outCols) * 18, cs.stack, cs.chance);
        }
        x += outCols * 18 + 4;
        if (!r.fluidOut.isEmpty()) {
            b.tank(x, 2, 16, 40, r.fluidOut, Math.max(1000, r.fluidOut.getAmount()), true);
            x += 20;
        }
        if (!r.fluidOut2.isEmpty()) {
            b.tank(x, 2, 16, 40, r.fluidOut2, Math.max(1000, r.fluidOut2.getAmount()), true);
        }
        String info = (r.energy > 0 ? (r.energy * r.time) + " FE  " : "") + String.format("%.1fs", r.time / 20f);
        b.text(2, 50, Component.literal(info), 0xFF606060);
        return b.build();
    }

    public static List<ItemStack> stacks(ItemStack... s) {
        return Arrays.asList(s);
    }
}
