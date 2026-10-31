package com.lodestar.dynamos;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/** Recipe viewer pages listing what each dynamo burns and how much energy it gives. */
public final class DynamosSetup {
    private DynamosSetup() {}

    public static void init(IEventBus bus) {
        items("stirling", Content.STIRLING_DYNAMO, DynamoFuels::stirling, List.of(Items.COAL, Items.CHARCOAL, Items.COAL_BLOCK, Items.BLAZE_ROD,
                Items.LAVA_BUCKET, Items.OAK_LOG, Items.OAK_PLANKS, Items.STICK, Items.DRIED_KELP_BLOCK, Items.BAMBOO), 300);
        items("lapidary", Content.LAPIDARY_DYNAMO, DynamoFuels::lapidary, List.of(Items.DIAMOND, Items.EMERALD, Items.ECHO_SHARD, Items.LAPIS_LAZULI,
                Items.QUARTZ, Items.AMETHYST_SHARD, Items.PRISMARINE_CRYSTALS, Items.PRISMARINE_SHARD), 303);
        items("redflux", Content.REDFLUX_DYNAMO, DynamoFuels::redflux, List.of(Items.REDSTONE, Items.REDSTONE_BLOCK, Items.GLOWSTONE_DUST, Items.GLOWSTONE), 304);
        ResourceLocation mag = new ResourceLocation("ls_dynamos", "magmatic");
        LensRegistry.register(cat(mag, Content.MAGMATIC_DYNAMO, 301), level -> List.of(fluidView(mag, Fluids.LAVA, 100)));
        ResourceLocation comp = new ResourceLocation("ls_dynamos", "compression");
        LensRegistry.register(cat(comp, Content.COMPRESSION_DYNAMO, 302), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (Fluid f : BuiltInRegistries.FLUID) {
                if (f == Fluids.EMPTY || !f.isSource(f.defaultFluidState())) continue;
                int v = DynamoFuels.compression(new FluidStack(f, 1));
                if (v > 0) out.add(fluidView(comp, f, v));
            }
            out.sort((a, b) -> a.texts.get(0).text().getString().compareTo(b.texts.get(0).text().getString()));
            return out;
        });
    }

    private static RecipeCategory cat(ResourceLocation id, Supplier<? extends ItemLike> dynamo, int order) {
        return new RecipeCategory(id, Component.translatable("lens.category." + id.getNamespace() + "." + id.getPath()),
                () -> new ItemStack(dynamo.get()), () -> List.of(new ItemStack(dynamo.get())), order);
    }

    private static void items(String name, Supplier<? extends ItemLike> dynamo, ToLongFunction<ItemStack> value, List<Item> shown, int order) {
        ResourceLocation id = new ResourceLocation("ls_dynamos", name);
        LensRegistry.register(cat(id, dynamo, order), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (Item it : shown) {
                long v = value.applyAsLong(new ItemStack(it));
                if (v <= 0) continue;
                out.add(RecipeView.builder(id, 140, 24).input(4, 4, new ItemStack(it))
                        .text(28, 8, Component.translatable("lens.ls_dynamos.fe", String.format("%,d", v))).build());
            }
            return out;
        });
    }

    private static RecipeView fluidView(ResourceLocation cat, Fluid f, int perMb) {
        return RecipeView.builder(cat, 140, 24).tank(4, 2, 16, 20, new FluidStack(f, 1000), 1000, false)
                .text(28, 3, f.getFluidType().getDescription())
                .text(28, 13, Component.translatable("lens.ls_dynamos.fe_bucket", String.format("%,d", perMb * 1000)), 0xFF606060).build();
    }
}
