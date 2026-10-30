package com.lodestar.jetpacks;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A jetpack plus one upgrade item anywhere in the crafting grid installs (or raises) that upgrade. */
public class JetpackUpgradeRecipe extends CustomRecipe {
    public JetpackUpgradeRecipe(ResourceLocation id, CraftingBookCategory cat) {
        super(id, cat);
    }

    private record Found(ItemStack pack, JetpackItem.Mod mod) {}

    @Nullable
    private Found find(CraftingContainer inv) {
        ItemStack pack = ItemStack.EMPTY;
        JetpackItem.Mod mod = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof JetpackItem) {
                if (!pack.isEmpty()) return null;
                pack = s;
            } else if (s.getItem() instanceof UpgradeItem u) {
                if (mod != null) return null;
                mod = u.mod;
            } else return null;
        }
        if (pack.isEmpty() || mod == null) return null;
        JetpackItem j = (JetpackItem) pack.getItem();
        if (JetpackItem.level(pack, mod) >= mod.max) return null;
        if (mod == JetpackItem.Mod.STABILIZER && j.tier.hover) return null;
        return new Found(pack, mod);
    }

    @Override
    public boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level level) {
        return find(inv) != null;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inv, @Nonnull RegistryAccess access) {
        Found f = find(inv);
        if (f == null) return ItemStack.EMPTY;
        ItemStack out = f.pack().copy();
        JetpackItem.setLevel(out, f.mod(), JetpackItem.level(out, f.mod()) + 1);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return JetpacksSetup.UPGRADE_SERIALIZER.get();
    }
}
