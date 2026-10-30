package com.lodestar.datastore.craft;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A 3x3 crafting container with no menu behind it, for encoding and running recipe cards. */
public final class GridContainer implements CraftingContainer {
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public GridContainer(ItemStack[] grid) {
        for (int i = 0; i < 9 && i < grid.length; i++) items.set(i, grid[i] == null ? ItemStack.EMPTY : grid[i].copy());
    }

    public GridContainer(List<ItemStack> grid) {
        for (int i = 0; i < 9 && i < grid.size(); i++) items.set(i, grid.get(i).copy());
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return i >= 0 && i < 9 ? items.get(i) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int i, int n) {
        return ContainerHelper.removeItem(items, i, n);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(items, i);
    }

    @Override
    public void setItem(int i, ItemStack s) {
        items.set(i, s);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player p) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void fillStackedContents(StackedContents c) {
        for (ItemStack s : items) c.accountSimpleStack(s);
    }
}
