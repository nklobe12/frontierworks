package com.lodestar.jetpacks;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A shaped recipe that carries the upgrades and switches of the jetpack used in it over to the next tier. */
public class TieredJetpackRecipe extends ShapedRecipe {
    public TieredJetpackRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY));
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inv, @Nonnull RegistryAccess access) {
        ItemStack out = super.assemble(inv, access);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof JetpackItem && s.getTag() != null) {
                CompoundTag t = out.getOrCreateTag();
                if (s.getTag().contains(JetpackItem.MODS)) {
                    CompoundTag mods = s.getTag().getCompound(JetpackItem.MODS).copy();
                    if (out.getItem() instanceof JetpackItem j && j.tier.hover) mods.remove(JetpackItem.Mod.STABILIZER.name());
                    t.put(JetpackItem.MODS, mods);
                }
                t.putBoolean(JetpackItem.HOVER, s.getTag().getBoolean(JetpackItem.HOVER));
            }
        }
        return out;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return JetpacksSetup.TIERED_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<TieredJetpackRecipe> {
        @Nonnull
        @Override
        public TieredJetpackRecipe fromJson(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            return new TieredJetpackRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Nullable
        @Override
        public TieredJetpackRecipe fromNetwork(@Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf buf) {
            ShapedRecipe r = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return r == null ? null : new TieredJetpackRecipe(r);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buf, @Nonnull TieredJetpackRecipe r) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, r);
        }
    }
}
