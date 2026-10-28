package com.lodestar.kinetics;

import com.lodestar.core.recipe.MachineRecipe;
import com.lodestar.core.recipe.MachineRecipeKind;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Hand-written setup for Kinetics: recipe viewer categories and self-test scenarios. */
public final class KineticsSetup {
    private KineticsSetup() {}

    public static void init(IEventBus bus) {
        lens(Content.GRINDING, 400, () -> new ItemStack(Content.GRIST_MILL.get()));
        lens(Content.STAMPING, 401, () -> new ItemStack(Content.STAMPING_PRESS.get()));
        lens(Content.MIXING, 402, () -> new ItemStack(Content.MIXING_VAT.get()));
        lens(Content.HEATED_MIXING, 403, () -> new ItemStack(Content.MIXING_VAT.get()), () -> new ItemStack(Items.CAMPFIRE),
                () -> new ItemStack(Items.MAGMA_BLOCK), () -> new ItemStack(Items.LAVA_BUCKET));
        lens(Content.RINSING, 404, () -> new ItemStack(Content.AIR_BLOWER.get()), () -> new ItemStack(Items.WATER_BUCKET));
        lens(Content.STEEPING, 405, () -> new ItemStack(Content.AIR_BLOWER.get()), () -> new ItemStack(Items.SOUL_CAMPFIRE));
        LensRegistry.addCatalyst(new ResourceLocation("minecraft", "smoking"), () -> new ItemStack(Content.AIR_BLOWER.get()));
        LensRegistry.addCatalyst(new ResourceLocation("minecraft", "smelting"), () -> new ItemStack(Content.AIR_BLOWER.get()));
        if (com.lodestar.core.test.SelfTest.enabled()) KineticsTests.register();
    }

    @SafeVarargs
    private static void lens(MachineRecipeKind kind, int order, Supplier<ItemStack>... catalysts) {
        ResourceLocation id = kind.id;
        Supplier<List<ItemStack>> cats = () -> {
            List<ItemStack> l = new ArrayList<>();
            for (Supplier<ItemStack> s : catalysts) l.add(s.get());
            return l;
        };
        LensRegistry.register(new RecipeCategory(id, Component.translatable("lens.category." + id.getNamespace() + "." + id.getPath()),
                catalysts[0], cats, order), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (MachineRecipe r : kind.all(level)) out.add(LensRegistry.machineView(id, r));
            return out;
        });
    }
}
