package com.lodestar.core.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;

/**
 * Lets any screen (Codex pages, machine GUIs) open the Lens recipe viewer without depending on it. Lens fills these
 * in on the client; until then they do nothing.
 */
public final class LensBridge {
    private LensBridge() {}

    public static Consumer<ItemStack> recipes = s -> {};
    public static Consumer<ItemStack> uses = s -> {};
    public static Consumer<FluidStack> fluidRecipes = f -> {};
    public static Consumer<FluidStack> fluidUses = f -> {};
    public static Consumer<ResourceLocation> category = c -> {};
    /** Whether the Lens overlay is installed. */
    public static boolean present;
}
