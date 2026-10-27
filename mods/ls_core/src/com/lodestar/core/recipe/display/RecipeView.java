package com.lodestar.core.recipe.display;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data-only description of how to show one recipe (or any input/output relation) in the Lens recipe viewer
 * and in Codex pages. Built on either side without client classes; Lens draws it generically.
 */
public final class RecipeView {
    public final ResourceLocation category;
    @Nullable
    public final ResourceLocation recipeId;
    public final int width, height;
    public final List<SlotView> slots;
    public final List<TankView> tanks;
    public final List<TextView> texts;
    public final int arrowX, arrowY;
    /** optional crafting-grid size for "fill the grid" transfer (0 = not transferable) */
    public final int gridWidth, gridHeight;

    private RecipeView(Builder b) {
        this.category = b.category;
        this.recipeId = b.recipeId;
        this.width = b.width;
        this.height = b.height;
        this.slots = b.slots;
        this.tanks = b.tanks;
        this.texts = b.texts;
        this.arrowX = b.arrowX;
        this.arrowY = b.arrowY;
        this.gridWidth = b.gridWidth;
        this.gridHeight = b.gridHeight;
    }

    public record SlotView(int x, int y, List<ItemStack> stacks, boolean output, float chance, @Nullable Component note) {}

    public record TankView(int x, int y, int w, int h, FluidStack fluid, int capacity, boolean output) {}

    public record TextView(int x, int y, Component text, int color, boolean centered) {}

    public boolean producesItem(ItemStack stack) {
        for (SlotView s : slots) if (s.output) for (ItemStack st : s.stacks) if (ItemStack.isSameItem(st, stack)) return true;
        return false;
    }

    public boolean usesItem(ItemStack stack) {
        for (SlotView s : slots) if (!s.output) for (ItemStack st : s.stacks) if (ItemStack.isSameItem(st, stack)) return true;
        return false;
    }

    public boolean producesFluid(FluidStack f) {
        for (TankView t : tanks) if (t.output && t.fluid.isFluidEqual(f)) return true;
        return false;
    }

    public boolean usesFluid(FluidStack f) {
        for (TankView t : tanks) if (!t.output && t.fluid.isFluidEqual(f)) return true;
        return false;
    }

    public static Builder builder(ResourceLocation category, int width, int height) {
        return new Builder(category, width, height);
    }

    public static final class Builder {
        private final ResourceLocation category;
        @Nullable
        private ResourceLocation recipeId;
        private final int width, height;
        private final List<SlotView> slots = new ArrayList<>();
        private final List<TankView> tanks = new ArrayList<>();
        private final List<TextView> texts = new ArrayList<>();
        private int arrowX = -1, arrowY = -1;
        private int gridWidth, gridHeight;

        Builder(ResourceLocation category, int width, int height) {
            this.category = category;
            this.width = width;
            this.height = height;
        }

        public Builder id(@Nullable ResourceLocation id) {
            this.recipeId = id;
            return this;
        }

        public Builder input(int x, int y, Ingredient ing) {
            return input(x, y, Arrays.asList(ing.getItems()));
        }

        public Builder input(int x, int y, List<ItemStack> stacks) {
            slots.add(new SlotView(x, y, stacks, false, 1f, null));
            return this;
        }

        public Builder input(int x, int y, ItemStack stack) {
            return input(x, y, List.of(stack));
        }

        public Builder output(int x, int y, ItemStack stack) {
            return output(x, y, stack, 1f);
        }

        public Builder output(int x, int y, ItemStack stack, float chance) {
            slots.add(new SlotView(x, y, List.of(stack), true, chance, null));
            return this;
        }

        public Builder outputs(int x, int y, List<ItemStack> stacks) {
            slots.add(new SlotView(x, y, stacks, true, 1f, null));
            return this;
        }

        /** A slot that is neither consumed nor produced (catalyst, cast, tool), with a note in its tooltip. */
        public Builder catalyst(int x, int y, List<ItemStack> stacks, Component note) {
            slots.add(new SlotView(x, y, stacks, false, 1f, note));
            return this;
        }

        public Builder tank(int x, int y, int w, int h, FluidStack fluid, int capacity, boolean output) {
            tanks.add(new TankView(x, y, w, h, fluid, Math.max(1, capacity), output));
            return this;
        }

        public Builder text(int x, int y, Component text) {
            texts.add(new TextView(x, y, text, 0xFF404040, false));
            return this;
        }

        public Builder textCentered(int x, int y, Component text) {
            texts.add(new TextView(x, y, text, 0xFF404040, true));
            return this;
        }

        public Builder text(int x, int y, Component text, int color) {
            texts.add(new TextView(x, y, text, color, false));
            return this;
        }

        public Builder arrow(int x, int y) {
            arrowX = x;
            arrowY = y;
            return this;
        }

        public Builder grid(int w, int h) {
            gridWidth = w;
            gridHeight = h;
            return this;
        }

        public RecipeView build() {
            return new RecipeView(this);
        }
    }
}
