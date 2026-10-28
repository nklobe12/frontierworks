package com.lodestar.quarry;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import com.lodestar.core.test.SelfTest;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Recipe viewer pages for drill bits, sieves and land restoration, and the self-test scenarios. */
public final class QuarrySetup {
    private QuarrySetup() {}

    public static void init(IEventBus bus) {
        ResourceLocation bits = new ResourceLocation("ls_quarry", "drill_bits");
        LensRegistry.register(cat(bits, Content.FORTUNE_DRILL_BIT_2, 420), level -> {
            List<RecipeView> out = new ArrayList<>();
            out.add(bitView(bits, Content.FORTUNE_DRILL_BIT_1, new ItemStack(Items.DIAMOND, 2), 1.5f));
            out.add(bitView(bits, Content.FORTUNE_DRILL_BIT_2, new ItemStack(Items.DIAMOND, 3), 2.0f));
            out.add(bitView(bits, Content.FORTUNE_DRILL_BIT_3, new ItemStack(Items.DIAMOND, 4), 3.0f));
            out.add(bitView(bits, Content.SILK_DRILL_BIT, new ItemStack(Items.DIAMOND_ORE), 2.0f));
            return out;
        });
        ResourceLocation sieves = new ResourceLocation("ls_quarry", "sieves");
        LensRegistry.register(cat(sieves, Content.ORE_SIEVE, 421), level -> List.of(
                sieveView(sieves, Content.ORE_SIEVE, List.of(Items.RAW_IRON, Items.RAW_COPPER, Items.COAL, Items.DIAMOND, Items.REDSTONE),
                        List.of(Items.COBBLESTONE, Items.DIRT, Items.GRAVEL, Items.ANDESITE, Items.FLINT)),
                sieveView(sieves, Content.RUBBLE_SCREEN, List.of(Items.RAW_IRON, Items.COAL, Items.CLAY_BALL, Items.AMETHYST_SHARD, Items.FLINT),
                        List.of(Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.COBBLED_DEEPSLATE))));
        ResourceLocation restore = new ResourceLocation("ls_quarry", "restoration");
        LensRegistry.register(cat(restore, Content.QUARRY, 422), level -> {
            List<ItemStack> fill = Arrays.asList(Ingredient.of(AbstractQuarryBE.FILL).getItems());
            RecipeView.Builder b = RecipeView.builder(restore, 150, 52).input(4, 14, fill.isEmpty() ? List.of(new ItemStack(Items.COBBLESTONE)) : fill)
                    .arrow(28, 14);
            Item[] made = {Items.GRASS_BLOCK, Items.DIRT, Items.STONE, Items.DEEPSLATE};
            for (int i = 0; i < made.length; i++) b.output(58 + i * 18, 14, new ItemStack(made[i]));
            b.text(4, 2, Component.translatable("lens.ls_quarry.fill"), 0xFF404040);
            b.text(4, 36, Component.translatable("lens.ls_quarry.spoil"), 0xFF606060);
            return List.of(b.build());
        });
        if (SelfTest.enabled()) QuarryTests.register();
    }

    private static RecipeCategory cat(ResourceLocation id, RegistryObject<? extends net.minecraft.world.level.ItemLike> icon, int order) {
        return new RecipeCategory(id, Component.translatable("lens.category." + id.getNamespace() + "." + id.getPath()),
                () -> new ItemStack(icon.get()), () -> List.of(new ItemStack(Content.QUARRY.get()), new ItemStack(Content.LASER_QUARRY.get())), order);
    }

    private static RecipeView bitView(ResourceLocation cat, RegistryObject<? extends Item> bit, ItemStack result, float energy) {
        return RecipeView.builder(cat, 150, 40)
                .catalyst(4, 4, List.of(new ItemStack(bit.get())), Component.translatable("lens.ls_core.not_consumed"))
                .input(30, 4, new ItemStack(Items.DIAMOND_ORE)).arrow(54, 4).output(86, 4, result)
                .text(4, 26, Component.translatable("lens.ls_quarry.energy", String.format("%.1f", energy)), 0xFF606060).build();
    }

    private static RecipeView sieveView(ResourceLocation cat, RegistryObject<? extends Item> sieve, List<Item> kept, List<Item> voided) {
        RecipeView.Builder b = RecipeView.builder(cat, 160, 46)
                .catalyst(4, 14, List.of(new ItemStack(sieve.get())), Component.translatable("lens.ls_core.not_consumed"));
        b.text(26, 6, Component.translatable("lens.ls_quarry.kept"), 0xFF2E7D32);
        b.text(26, 28, Component.translatable("lens.ls_quarry.voided"), 0xFF9E2A2A);
        for (int i = 0; i < kept.size(); i++) b.output(70 + i * 18, 2, new ItemStack(kept.get(i)));
        for (int i = 0; i < voided.size(); i++) b.input(70 + i * 18, 24, new ItemStack(voided.get(i)));
        return b.build();
    }
}
