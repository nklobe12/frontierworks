package com.lodestar.lens.client;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every recipe view of every category, built on first use and rebuilt when recipes or tags change. */
public final class RecipeIndex {
    private static final Logger LOG = LogUtils.getLogger();
    @Nullable
    private static Map<RecipeCategory, List<RecipeView>> all;

    private RecipeIndex() {}

    public static void clear() {
        all = null;
    }

    public static Map<RecipeCategory, List<RecipeView>> all() {
        if (all != null) return all;
        Map<RecipeCategory, List<RecipeView>> m = new LinkedHashMap<>();
        if (Minecraft.getInstance().level == null) return m;
        long t0 = System.currentTimeMillis();
        int n = 0;
        for (RecipeCategory c : LensRegistry.categories()) {
            try {
                List<RecipeView> views = LensRegistry.provider(c.id).views(Minecraft.getInstance().level);
                m.put(c, views);
                n += views.size();
            } catch (Throwable t) {
                LOG.warn("Lens: category {} failed to list its recipes", c.id, t);
            }
        }
        LOG.info("Lens indexed {} recipes in {} categories ({} ms)", n, m.size(), System.currentTimeMillis() - t0);
        all = m;
        return m;
    }

    public enum Mode { RECIPES, USES, CATEGORY }

    /** A tab of results: a category and the views in it that match. */
    public record Tab(RecipeCategory category, List<RecipeView> views) {}

    private static boolean isCatalyst(RecipeCategory c, ItemStack s) {
        for (ItemStack k : c.catalysts()) if (ItemStack.isSameItem(k, s)) return true;
        return false;
    }

    public static List<Tab> lookup(Mode mode, ItemStack item, FluidStack fluid, @Nullable net.minecraft.resources.ResourceLocation category) {
        List<Tab> out = new ArrayList<>();
        for (Map.Entry<RecipeCategory, List<RecipeView>> e : all().entrySet()) {
            RecipeCategory c = e.getKey();
            List<RecipeView> views;
            if (mode == Mode.CATEGORY) {
                if (!c.id.equals(category)) continue;
                views = e.getValue();
            } else if (mode == Mode.USES && !item.isEmpty() && isCatalyst(c, item)) {
                views = e.getValue();
            } else {
                views = new ArrayList<>();
                for (RecipeView v : e.getValue()) {
                    boolean match;
                    if (!item.isEmpty()) match = mode == Mode.RECIPES ? v.producesItem(item) : v.usesItem(item);
                    else match = mode == Mode.RECIPES ? v.producesFluid(fluid) : v.usesFluid(fluid);
                    if (match) views.add(v);
                }
            }
            if (!views.isEmpty()) out.add(new Tab(c, views));
        }
        return out;
    }
}
