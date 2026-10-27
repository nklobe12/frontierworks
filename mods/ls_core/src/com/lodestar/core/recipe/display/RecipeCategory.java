package com.lodestar.core.recipe.display;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A tab in the Lens recipe viewer: title, icon and the blocks or items that perform its recipes ("catalysts").
 * Its recipes come from a {@link LensRegistry.Provider}.
 */
public final class RecipeCategory {
    public final ResourceLocation id;
    public final Component title;
    private final Supplier<ItemStack> icon;
    private final Supplier<List<ItemStack>> catalysts;
    /** lower sorts first */
    public final int order;

    public RecipeCategory(ResourceLocation id, Component title, Supplier<ItemStack> icon, Supplier<List<ItemStack>> catalysts, int order) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.catalysts = catalysts;
        this.order = order;
    }

    public ItemStack icon() {
        return icon.get();
    }

    public List<ItemStack> catalysts() {
        return catalysts.get();
    }
}
