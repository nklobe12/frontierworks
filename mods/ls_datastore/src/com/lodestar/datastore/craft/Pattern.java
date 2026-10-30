package com.lodestar.datastore.craft;

import com.lodestar.datastore.ItemKey;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A recipe card read back into a usable recipe: the exact 3x3 layout it was encoded with, its output, and for each slot
 * the items that may stand in for the encoded one when substitutes are allowed.
 */
public final class Pattern {
    /** One kind of ingredient for one craft: any of alts, count times. */
    public record Req(List<ItemKey> alts, int count) {}

    /** What one craft produced. */
    public record Result(ItemStack output, List<ItemStack> remainders) {}

    public final ItemKey cardKey;
    public final ItemStack card;
    @Nullable
    public final ResourceLocation recipeId;
    public final ItemStack[] grid = new ItemStack[9];
    public final ItemKey output;
    public final int outCount;
    public final boolean substitute;
    public final List<List<ItemKey>> slotAlts = new ArrayList<>();
    public final List<Req> reqs = new ArrayList<>();

    private Pattern(ItemStack card, CompoundTag tag, Level level) {
        this.card = card.copyWithCount(1);
        this.cardKey = ItemKey.of(this.card);
        ListTag g = tag.getList("Grid", Tag.TAG_COMPOUND);
        for (int i = 0; i < 9; i++) grid[i] = i < g.size() ? ItemStack.of(g.getCompound(i)) : ItemStack.EMPTY;
        ItemStack out = ItemStack.of(tag.getCompound("Out"));
        this.output = ItemKey.of(out);
        this.outCount = Math.max(1, out.getCount());
        this.substitute = tag.getBoolean("Sub");
        this.recipeId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        Ingredient[] ings = substitute ? slotIngredients(level) : new Ingredient[9];
        for (int i = 0; i < 9; i++) {
            if (grid[i].isEmpty()) {
                slotAlts.add(List.of());
                continue;
            }
            LinkedHashSet<ItemKey> alts = new LinkedHashSet<>();
            alts.add(ItemKey.of(grid[i]));
            if (ings[i] != null) {
                for (ItemStack s : ings[i].getItems()) {
                    if (alts.size() >= 48) break;
                    if (!s.isEmpty()) alts.add(ItemKey.of(s));
                }
            }
            slotAlts.add(List.copyOf(alts));
        }
        Map<List<ItemKey>, Integer> grouped = new LinkedHashMap<>();
        for (List<ItemKey> a : slotAlts) if (!a.isEmpty()) grouped.merge(a, 1, Integer::sum);
        for (Map.Entry<List<ItemKey>, Integer> e : grouped.entrySet()) reqs.add(new Req(e.getKey(), e.getValue()));
    }

    @Nullable
    public static Pattern of(ItemStack card, Level level) {
        CompoundTag t = card.getTag();
        if (t == null || !t.contains("Out") || !t.contains("Grid")) return null;
        Pattern p = new Pattern(card, t, level);
        return p.output == null || p.reqs.isEmpty() ? null : p;
    }

    /** The recipe ingredient each grid slot stands for (null where unknown). */
    private Ingredient[] slotIngredients(Level level) {
        Ingredient[] out = new Ingredient[9];
        CraftingRecipe r = recipe(level);
        if (r == null) return out;
        NonNullList<Ingredient> ings = r.getIngredients();
        if (r instanceof IShapedRecipe<?> sr) {
            int w = sr.getRecipeWidth(), h = sr.getRecipeHeight();
            int ox = 3, oy = 3;
            for (int i = 0; i < 9; i++) {
                if (grid[i].isEmpty()) continue;
                ox = Math.min(ox, i % 3);
                oy = Math.min(oy, i / 3);
            }
            for (int i = 0; i < 9; i++) {
                if (grid[i].isEmpty()) continue;
                int x = i % 3 - ox, y = i / 3 - oy;
                if (x >= w || y >= h) continue;
                Ingredient a = ings.get(x + y * w), b = ings.get(w - 1 - x + y * w);
                out[i] = a.test(grid[i]) ? a : b.test(grid[i]) ? b : null;
            }
        } else {
            boolean[] used = new boolean[ings.size()];
            for (int i = 0; i < 9; i++) {
                if (grid[i].isEmpty()) continue;
                for (int j = 0; j < ings.size(); j++) {
                    if (!used[j] && ings.get(j).test(grid[i])) {
                        used[j] = true;
                        out[i] = ings.get(j);
                        break;
                    }
                }
            }
        }
        return out;
    }

    @Nullable
    public CraftingRecipe recipe(Level level) {
        if (recipeId == null) return null;
        Optional<? extends Recipe<?>> r = level.getRecipeManager().byKey(recipeId);
        return r.isPresent() && r.get() instanceof CraftingRecipe cr ? cr : null;
    }

    /** Takes one craft's worth of items out of the buffer (choosing substitutes as needed); null when they are not all there. */
    @Nullable
    public ItemStack[] take(Map<ItemKey, Long> buffer) {
        Map<ItemKey, Long> reserved = new LinkedHashMap<>();
        ItemKey[] chosen = new ItemKey[9];
        for (int i = 0; i < 9; i++) {
            List<ItemKey> alts = slotAlts.get(i);
            if (alts.isEmpty()) continue;
            for (ItemKey k : alts) {
                long have = buffer.getOrDefault(k, 0L) - reserved.getOrDefault(k, 0L);
                if (have > 0) {
                    chosen[i] = k;
                    reserved.merge(k, 1L, Long::sum);
                    break;
                }
            }
            if (chosen[i] == null) return null;
        }
        ItemStack[] items = new ItemStack[9];
        for (int i = 0; i < 9; i++) items[i] = chosen[i] == null ? ItemStack.EMPTY : chosen[i].stack(1);
        for (Map.Entry<ItemKey, Long> e : reserved.entrySet()) {
            long left = buffer.get(e.getKey()) - e.getValue();
            if (left <= 0) buffer.remove(e.getKey());
            else buffer.put(e.getKey(), left);
        }
        return items;
    }

    /** Runs the recipe on concrete items. Null if they no longer make this card's output. */
    @Nullable
    public Result craft(Level level, ItemStack[] items) {
        GridContainer c = new GridContainer(items);
        Optional<CraftingRecipe> r = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, c, level);
        if (r.isEmpty()) return null;
        ItemStack res = r.get().assemble(c, level.registryAccess());
        if (res.isEmpty() || !output.equals(ItemKey.of(res))) return null;
        List<ItemStack> rem = new ArrayList<>();
        for (ItemStack s : r.get().getRemainingItems(c)) if (!s.isEmpty()) rem.add(s.copy());
        return new Result(res, rem);
    }

    /** Writes a card for the recipe the grid makes; EMPTY if the grid is no crafting recipe. */
    public static ItemStack encode(Level level, List<ItemStack> grid, boolean substitute, ItemStack cardItem) {
        GridContainer c = new GridContainer(grid);
        Optional<CraftingRecipe> r = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, c, level);
        if (r.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = r.get().assemble(c, level.registryAccess());
        if (out.isEmpty()) return ItemStack.EMPTY;
        ItemStack card = cardItem.copyWithCount(1);
        CompoundTag t = new CompoundTag();
        ListTag g = new ListTag();
        for (int i = 0; i < 9; i++) {
            ItemStack s = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            g.add(s.isEmpty() ? new CompoundTag() : s.copyWithCount(1).save(new CompoundTag()));
        }
        t.put("Grid", g);
        t.put("Out", out.save(new CompoundTag()));
        t.putString("Recipe", r.get().getId().toString());
        t.putBoolean("Sub", substitute);
        card.setTag(t);
        return card;
    }

    /** Output preview of an encoded card (client safe). */
    public static ItemStack outputOf(ItemStack card) {
        CompoundTag t = card.getTag();
        return t == null || !t.contains("Out") ? ItemStack.EMPTY : ItemStack.of(t.getCompound("Out"));
    }
}
