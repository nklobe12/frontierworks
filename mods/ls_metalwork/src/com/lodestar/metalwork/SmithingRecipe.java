package com.lodestar.metalwork;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Smithing anvil recipe: a hot workpiece is shaped into a result by hammer strikes.
 * JSON: {"type":"ls_metalwork:smithing","input":{...},"shape":"plate","strikes":3,"min_heat":600,"result":{"item":"...","count":1}}
 */
public class SmithingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    public final Ingredient input;
    public final String shape;
    public final int strikes;
    public final int minHeat;
    public final ItemStack result;

    public SmithingRecipe(ResourceLocation id, Ingredient input, String shape, int strikes, int minHeat, ItemStack result) {
        this.id = id;
        this.input = input;
        this.shape = shape;
        this.strikes = Math.max(1, strikes);
        this.minHeat = minHeat;
        this.result = result;
    }

    public static List<SmithingRecipe> forItem(Level level, ItemStack stack) {
        List<SmithingRecipe> out = new ArrayList<>();
        for (SmithingRecipe r : level.getRecipeManager().getAllRecipesFor(MetalworkSetup.SMITHING_TYPE.get())) {
            if (r.input.test(stack)) out.add(r);
        }
        out.sort((a, b) -> a.shape.compareTo(b.shape));
        return out;
    }

    @Nullable
    public static SmithingRecipe find(Level level, ItemStack stack, String shape) {
        for (SmithingRecipe r : forItem(level, stack)) if (r.shape.equals(shape)) return r;
        return null;
    }

    @Override
    public boolean matches(Container c, Level level) {
        return input.test(c.getItem(0));
    }

    @Override
    public ItemStack assemble(Container c, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MetalworkSetup.SMITHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return MetalworkSetup.SMITHING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<SmithingRecipe> {
        @Override
        public SmithingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient in = Ingredient.fromJson(json.get("input"));
            String shape = GsonHelper.getAsString(json, "shape");
            int strikes = GsonHelper.getAsInt(json, "strikes", 3);
            int minHeat = GsonHelper.getAsInt(json, "min_heat", 600);
            ItemStack res = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new SmithingRecipe(id, in, shape, strikes, minHeat, res);
        }

        @Override
        public SmithingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient in = Ingredient.fromNetwork(buf);
            String shape = buf.readUtf();
            int strikes = buf.readVarInt();
            int minHeat = buf.readVarInt();
            ItemStack res = buf.readItem();
            return new SmithingRecipe(id, in, shape, strikes, minHeat, res);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SmithingRecipe r) {
            r.input.toNetwork(buf);
            buf.writeUtf(r.shape);
            buf.writeVarInt(r.strikes);
            buf.writeVarInt(r.minHeat);
            buf.writeItem(r.result);
        }
    }
}
