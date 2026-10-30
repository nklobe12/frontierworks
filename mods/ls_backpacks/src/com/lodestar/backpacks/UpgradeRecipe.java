package com.lodestar.backpacks;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nonnull;

/** Shaped recipe that carries the backpack in the grid (its id, colour and name) over to the bigger backpack. */
public class UpgradeRecipe extends ShapedRecipe {
    public UpgradeRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(), base.getIngredients(),
                base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer grid, @Nonnull RegistryAccess access) {
        ItemStack out = super.assemble(grid, access);
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            if (s.getItem() instanceof BackpackItem && s.hasTag()) {
                out.setTag(s.getTag().copy());
                break;
            }
        }
        return out;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return BackpacksSetup.UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<UpgradeRecipe> {
        @Nonnull
        @Override
        public UpgradeRecipe fromJson(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            return new UpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public UpgradeRecipe fromNetwork(@Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf buf) {
            return new UpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buf, @Nonnull UpgradeRecipe r) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, r);
        }
    }
}
