package com.lodestar.codex.client;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds recipes that make an item, for the Codex's inline recipe: every Lens category (machine recipes and anything other
 * mods register), plus vanilla crafting, cooking and stonecutting drawn by the Codex itself when no Lens category covers
 * them. Views are computed once per category per Codex session.
 */
public final class RecipeFinder {
    private static final Logger LOG = LogUtils.getLogger();

    /** A recipe that makes the item, with the title and icon of where it is made. */
    public record Found(Component title, ItemStack icon, RecipeView view) {}

    private final Map<ResourceLocation, List<RecipeView>> cache = new HashMap<>();
    private final Map<net.minecraft.world.item.Item, List<Found>> results = new HashMap<>();

    public List<Found> producing(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        return results.computeIfAbsent(stack.getItem(), i -> search(stack));
    }

    private List<Found> search(ItemStack stack) {
        Level level = Minecraft.getInstance().level;
        List<Found> out = new ArrayList<>();
        if (level == null) return out;
        boolean lensCoversVanilla = false;
        List<RecipeCategory> cats = LensRegistry.categories();
        for (RecipeCategory c : cats) if (c.id.getNamespace().equals("minecraft")) lensCoversVanilla = true;
        if (!lensCoversVanilla) out.addAll(vanilla(level, stack));
        for (RecipeCategory c : cats) {
            List<RecipeView> views = cache.computeIfAbsent(c.id, id -> {
                try {
                    LensRegistry.Provider p = LensRegistry.provider(id);
                    return p == null ? List.of() : p.views(level);
                } catch (Exception e) {
                    LOG.warn("Codex: recipe category {} failed", id, e);
                    return List.of();
                }
            });
            ItemStack icon;
            try {
                icon = c.icon();
            } catch (Exception e) {
                icon = ItemStack.EMPTY;
            }
            for (RecipeView v : views) if (v.producesItem(stack)) out.add(new Found(c.title, icon, v));
        }
        return out;
    }

    private static final ResourceLocation CRAFTING = new ResourceLocation("minecraft", "crafting");

    private static List<Found> vanilla(Level level, ItemStack stack) {
        List<Found> out = new ArrayList<>();
        var rm = level.getRecipeManager();
        var ra = level.registryAccess();
        for (CraftingRecipe r : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack res;
            try {
                res = r.getResultItem(ra);
            } catch (Exception e) {
                continue;
            }
            if (res.isEmpty() || !ItemStack.isSameItem(res, stack)) continue;
            out.add(new Found(Component.translatable("gui.ls_codex.cat.crafting"), new ItemStack(Items.CRAFTING_TABLE), crafting(r, res)));
        }
        cooking(out, level, stack, RecipeType.SMELTING, "smelting", Items.FURNACE);
        cooking(out, level, stack, RecipeType.BLASTING, "blasting", Items.BLAST_FURNACE);
        cooking(out, level, stack, RecipeType.SMOKING, "smoking", Items.SMOKER);
        cooking(out, level, stack, RecipeType.CAMPFIRE_COOKING, "campfire", Items.CAMPFIRE);
        for (StonecutterRecipe r : rm.getAllRecipesFor(RecipeType.STONECUTTING)) {
            ItemStack res = r.getResultItem(ra);
            if (!ItemStack.isSameItem(res, stack)) continue;
            RecipeView.Builder b = RecipeView.builder(new ResourceLocation("minecraft", "stonecutting"), 90, 26).id(r.getId());
            b.input(4, 5, r.getIngredients().get(0));
            b.arrow(30, 5);
            b.output(64, 5, res);
            out.add(new Found(Component.translatable("gui.ls_codex.cat.stonecutting"), new ItemStack(Items.STONECUTTER), b.build()));
        }
        return out;
    }

    private static <T extends AbstractCookingRecipe> void cooking(List<Found> out, Level level, ItemStack stack, RecipeType<T> type, String name,
                                                                net.minecraft.world.item.Item icon) {
        for (T r : level.getRecipeManager().getAllRecipesFor(type)) {
            ItemStack res = r.getResultItem(level.registryAccess());
            if (!ItemStack.isSameItem(res, stack)) continue;
            RecipeView.Builder b = RecipeView.builder(new ResourceLocation("minecraft", name), 110, 36).id(r.getId());
            b.input(4, 5, r.getIngredients().get(0));
            b.arrow(30, 5);
            b.output(64, 5, res);
            String secs = String.format("%.1f", r.getCookingTime() / 20f);
            b.text(4, 27, Component.translatable("gui.ls_codex.cook_time", secs, String.format("%.1f", r.getExperience())), 0xFF6B5A44);
            out.add(new Found(Component.translatable("gui.ls_codex.cat." + name), new ItemStack(icon), b.build()));
        }
    }

    /** A 3x3 crafting grid (shaped recipes keep their layout), an arrow and the result. */
    static RecipeView crafting(CraftingRecipe r, ItemStack result) {
        NonNullList<Ingredient> ings = r.getIngredients();
        boolean shaped = r instanceof IShapedRecipe<?>;
        int w = shaped ? ((IShapedRecipe<?>) r).getRecipeWidth() : (ings.size() <= 4 ? 2 : 3);
        RecipeView.Builder b = RecipeView.builder(CRAFTING, 118, shaped ? 56 : 66).id(r.getId()).grid(3, 3);
        Ingredient[] grid = new Ingredient[9];
        for (int i = 0; i < ings.size() && i < 9; i++) {
            int x = i % w, y = i / w;
            if (x < 3 && y < 3) grid[y * 3 + x] = ings.get(i);
        }
        for (int i = 0; i < 9; i++) {
            int x = 1 + (i % 3) * 18, y = 1 + (i / 3) * 18;
            if (grid[i] == null || grid[i].isEmpty()) {
                b.input(x, y, List.of());
            } else {
                b.input(x, y, grid[i]);
            }
        }
        b.arrow(61, 19);
        b.output(95, 19, result);
        if (!shaped) b.text(1, 57, Component.translatable("gui.ls_codex.shapeless"), 0xFF6B5A44);
        return b.build();
    }
}
