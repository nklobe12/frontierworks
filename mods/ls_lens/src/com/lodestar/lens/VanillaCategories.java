package com.lodestar.lens;

import com.lodestar.core.recipe.RecipeUtil;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Lens categories for vanilla recipe types: crafting, the four cookers, stonecutting, smithing, fuel and composting. */
public final class VanillaCategories {
    private VanillaCategories() {}

    public static final ResourceLocation CRAFTING = new ResourceLocation("minecraft", "crafting");
    public static final ResourceLocation SMELTING = new ResourceLocation("minecraft", "smelting");

    private static RecipeCategory cat(String path, ItemLike icon, int order, ItemLike... catalysts) {
        ResourceLocation id = new ResourceLocation("minecraft", path);
        return new RecipeCategory(id, Component.translatable("lens.category.minecraft." + path), () -> new ItemStack(icon),
                () -> {
                    List<ItemStack> l = new ArrayList<>();
                    for (ItemLike c : catalysts) l.add(new ItemStack(c));
                    l.addAll(LensRegistry.extraCatalysts(id));
                    return l;
                }, order);
    }

    private static List<ItemStack> stacks(Ingredient ing) {
        return Arrays.asList(ing.getItems());
    }

    public static void register() {
        LensRegistry.register(cat("crafting", Items.CRAFTING_TABLE, 0, Items.CRAFTING_TABLE), level -> {
            List<RecipeView> out = new ArrayList<>();
            RegistryAccess ra = level.registryAccess();
            for (CraftingRecipe r : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack result;
                try {
                    result = r.getResultItem(ra);
                } catch (Exception e) {
                    continue;
                }
                NonNullList<Ingredient> ings = r.getIngredients();
                if (result.isEmpty() || ings.isEmpty()) continue;
                RecipeView.Builder b = RecipeView.builder(CRAFTING, 118, 56).id(r.getId());
                List<List<ItemStack>> cells = new ArrayList<>();
                for (int i = 0; i < 9; i++) cells.add(List.of());
                int w, h;
                boolean shapeless = !(r instanceof IShapedRecipe<?>);
                if (!shapeless) {
                    IShapedRecipe<?> sr = (IShapedRecipe<?>) r;
                    w = sr.getRecipeWidth();
                    h = sr.getRecipeHeight();
                    for (int y = 0; y < h && y < 3; y++)
                        for (int x = 0; x < w && x < 3; x++) {
                            int i = y * w + x;
                            if (i < ings.size() && !ings.get(i).isEmpty()) cells.set(y * 3 + x, stacks(ings.get(i)));
                        }
                } else {
                    int n = Math.min(9, ings.size());
                    w = h = n <= 1 ? 1 : n <= 4 ? 2 : 3;
                    for (int i = 0; i < n; i++) if (!ings.get(i).isEmpty()) cells.set((i / w) * 3 + i % w, stacks(ings.get(i)));
                    b.text(62, 42, Component.translatable("lens.ls_lens.shapeless"), 0xFF707070);
                }
                for (int i = 0; i < 9; i++) b.input(2 + (i % 3) * 18, 2 + (i / 3) * 18, cells.get(i));
                int g = Math.max(w, h) <= 2 ? 2 : 3;
                b.arrow(62, 19).output(96, 19, result).grid(g, g);
                out.add(b.build());
            }
            return out;
        });
        cooking("smelting", RecipeType.SMELTING, Items.FURNACE, 10);
        cooking("blasting", RecipeType.BLASTING, Items.BLAST_FURNACE, 11);
        cooking("smoking", RecipeType.SMOKING, Items.SMOKER, 12);
        cooking("campfire_cooking", RecipeType.CAMPFIRE_COOKING, Items.CAMPFIRE, 13);
        LensRegistry.register(cat("stonecutting", Items.STONECUTTER, 14, Items.STONECUTTER), level -> {
            List<RecipeView> out = new ArrayList<>();
            ResourceLocation id = new ResourceLocation("minecraft", "stonecutting");
            for (StonecutterRecipe r : level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING)) {
                if (r.getIngredients().isEmpty()) continue;
                out.add(RecipeView.builder(id, 90, 22).id(r.getId()).input(4, 3, stacks(r.getIngredients().get(0))).arrow(30, 2)
                        .output(64, 3, r.getResultItem(level.registryAccess())).build());
            }
            return out;
        });
        LensRegistry.register(cat("smithing", Items.SMITHING_TABLE, 15, Items.SMITHING_TABLE), level -> {
            List<RecipeView> out = new ArrayList<>();
            ResourceLocation id = new ResourceLocation("minecraft", "smithing");
            for (Recipe<?> r : level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)) {
                List<Ingredient> in = RecipeUtil.smithingInputs(r);
                ItemStack result = r.getResultItem(level.registryAccess());
                if (in.size() != 3 || result.isEmpty()) continue;
                out.add(RecipeView.builder(id, 128, 22).id(r.getId()).input(2, 3, stacks(in.get(0))).input(20, 3, stacks(in.get(1)))
                        .input(38, 3, stacks(in.get(2))).arrow(64, 2).output(100, 3, result).build());
            }
            return out;
        });
        LensRegistry.register(cat("fuel", Items.COAL, 20, Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER), level -> {
            List<RecipeView> out = new ArrayList<>();
            ResourceLocation id = new ResourceLocation("minecraft", "fuel");
            for (Item item : ForgeRegistries.ITEMS) {
                ItemStack s = new ItemStack(item);
                int t;
                try {
                    t = ForgeHooks.getBurnTime(s, RecipeType.SMELTING);
                } catch (Exception e) {
                    continue;
                }
                if (t <= 0) continue;
                out.add(RecipeView.builder(id, 130, 22).input(2, 3, s)
                        .text(24, 7, Component.translatable("lens.ls_lens.burns", String.format("%.1f", t / 200f), String.format("%.0f", t / 20f)), 0xFF505050).build());
            }
            return out;
        });
        LensRegistry.register(cat("composting", Items.COMPOSTER, 21, Items.COMPOSTER), level -> {
            List<RecipeView> out = new ArrayList<>();
            ResourceLocation id = new ResourceLocation("minecraft", "composting");
            ComposterBlock.COMPOSTABLES.forEach((item, chance) -> {
                if (chance <= 0) return;
                out.add(RecipeView.builder(id, 110, 22).input(2, 3, new ItemStack(item)).arrow(26, 2)
                        .text(56, 7, Component.translatable("lens.ls_lens.compost", Math.round(chance * 100)), 0xFF505050).build());
            });
            return out;
        });
    }

    private static void cooking(String path, RecipeType<? extends AbstractCookingRecipe> type, Item block, int order) {
        LensRegistry.register(cat(path, block, order, block), level -> {
            List<RecipeView> out = new ArrayList<>();
            ResourceLocation id = new ResourceLocation("minecraft", path);
            for (AbstractCookingRecipe r : level.getRecipeManager().getAllRecipesFor(type)) {
                if (r.getIngredients().isEmpty()) continue;
                String info = String.format("%.1fs", r.getCookingTime() / 20f) + (r.getExperience() > 0 ? String.format("  %.1f xp", r.getExperience()) : "");
                out.add(RecipeView.builder(id, 110, 38).id(r.getId()).input(4, 3, stacks(r.getIngredients().get(0))).arrow(30, 2)
                        .output(64, 3, r.getResultItem(level.registryAccess())).text(4, 26, Component.literal(info), 0xFF606060).build());
            }
            return out;
        });
    }
}
