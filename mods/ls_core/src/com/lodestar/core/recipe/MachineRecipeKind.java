package com.lodestar.core.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.List;

/** A machine recipe type plus its serializer, registered together by LSRegistry#machineRecipe. */
public class MachineRecipeKind {
    public final ResourceLocation id;
    private final RegistryObject<RecipeType<MachineRecipe>> type;
    private RegistryObject<RecipeSerializer<MachineRecipe>> serializer;

    public MachineRecipeKind(ResourceLocation id, RegistryObject<RecipeType<MachineRecipe>> type) {
        this.id = id;
        this.type = type;
    }

    public void setSerializer(RegistryObject<RecipeSerializer<MachineRecipe>> serializer) {
        this.serializer = serializer;
    }

    public RecipeType<MachineRecipe> type() {
        return type.get();
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return serializer.get();
    }

    public List<MachineRecipe> all(Level level) {
        if (level == null) {
            return Collections.emptyList();
        }
        return level.getRecipeManager().getAllRecipesFor(type.get());
    }
}
