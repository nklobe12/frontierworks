package com.lodestar.lens;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server side of "fill the grid": clears the open crafting grid back into the inventory, then lays out the recipe's
 * ingredients from the inventory (one craft, or as many as possible when asked).
 */
public final class Transfer {
    private Transfer() {}

    /** Grid slot indices (row-major) and grid width of the open menu, or null. */
    private static int[] grid(AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu) return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 3};
        if (menu instanceof InventoryMenu) return new int[]{1, 2, 3, 4, 2};
        return null;
    }

    public static void handle(ServerPlayer player, CompoundTag tag) {
        AbstractContainerMenu menu = player.containerMenu;
        int[] g = grid(menu);
        if (g == null) return;
        int gw = g[g.length - 1];
        Optional<? extends Recipe<?>> opt = player.server.getRecipeManager().byKey(new ResourceLocation(tag.getString("Recipe")));
        if (opt.isEmpty() || !(opt.get() instanceof CraftingRecipe r)) return;
        // lay the ingredients out on a gw x gw grid
        Ingredient[] layout = new Ingredient[gw * gw];
        NonNullList<Ingredient> ings = r.getIngredients();
        if (r instanceof IShapedRecipe<?> sr) {
            if (sr.getRecipeWidth() > gw || sr.getRecipeHeight() > gw) return;
            for (int y = 0; y < sr.getRecipeHeight(); y++)
                for (int x = 0; x < sr.getRecipeWidth(); x++) layout[y * gw + x] = ings.get(y * sr.getRecipeWidth() + x);
        } else {
            int n = 0;
            for (Ingredient i : ings) if (!i.isEmpty()) n++;
            if (n > gw * gw) return;
            int k = 0;
            for (Ingredient i : ings) if (!i.isEmpty()) layout[k++] = i;
        }
        Inventory inv = player.getInventory();
        // 1. clear the grid
        for (int i = 0; i < g.length - 1; i++) {
            Slot s = menu.getSlot(g[i]);
            ItemStack st = s.getItem();
            if (st.isEmpty()) continue;
            if (!inv.add(st.copy())) {
                menu.broadcastChanges();
                return;
            }
            s.set(ItemStack.EMPTY);
        }
        // 2. fill, one craft at a time
        int crafts = tag.getBoolean("Max") ? 64 : 1;
        for (int c = 0; c < crafts; c++) {
            List<Runnable> undo = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < layout.length && ok; i++) {
                Ingredient ing = layout[i];
                if (ing == null || ing.isEmpty()) continue;
                Slot slot = menu.getSlot(g[i]);
                ItemStack have = slot.getItem();
                if (!have.isEmpty() && have.getCount() >= have.getMaxStackSize()) {
                    ok = false;
                    break;
                }
                int found = -1;
                for (int j = 0; j < inv.items.size(); j++) {
                    ItemStack s = inv.items.get(j);
                    if (s.isEmpty() || !ing.test(s)) continue;
                    if (!have.isEmpty() && !ItemStack.isSameItemSameTags(have, s)) continue;
                    found = j;
                    break;
                }
                if (found < 0) {
                    ok = false;
                    break;
                }
                ItemStack src = inv.items.get(found);
                ItemStack one = src.split(1);
                final int fj = found;
                final ItemStack before = have.copy();
                if (have.isEmpty()) slot.set(one);
                else {
                    have.grow(1);
                    slot.set(have);
                }
                undo.add(() -> {
                    slot.set(before);
                    ItemStack back = inv.items.get(fj);
                    if (back.isEmpty()) inv.items.set(fj, one);
                    else back.grow(1);
                });
            }
            if (!ok) {
                for (int u = undo.size() - 1; u >= 0; u--) undo.get(u).run();
                break;
            }
        }
        menu.slotsChanged(inv);
        menu.broadcastChanges();
    }
}
