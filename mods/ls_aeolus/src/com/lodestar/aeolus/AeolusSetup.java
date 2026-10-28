package com.lodestar.aeolus;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Lens pages for rotors, the water wheel and the tidal generator; self-test scenarios. */
public final class AeolusSetup {
    private AeolusSetup() {}

    public static List<RegistryObject<RotorItem>> rotors() {
        return List.of(Content.WOODEN_ROTOR, Content.CANVAS_ROTOR, Content.BRONZE_ROTOR, Content.STEEL_ROTOR, Content.DURALUMIN_ROTOR);
    }

    public static void init(IEventBus bus) {
        ResourceLocation rot = new ResourceLocation("ls_aeolus", "rotors");
        LensRegistry.register(cat(rot, Content.WIND_TURBINE, 320), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (RegistryObject<RotorItem> ro : rotors()) {
                RotorItem r = ro.get();
                ItemStack st = new ItemStack(r);
                out.add(RecipeView.builder(rot, 150, 26).input(4, 5, st)
                        .text(26, 3, Component.translatable("lens.ls_aeolus.rotor", r.radius, r.blades))
                        .text(26, 14, Component.translatable("lens.ls_aeolus.rotor2", r.power, RotorItem.duration(st.getMaxDamage())), 0xFF606060)
                        .build());
            }
            return out;
        });
        ResourceLocation wheel = new ResourceLocation("ls_aeolus", "water_wheel");
        LensRegistry.register(cat(wheel, Content.WATER_WHEEL, 321), level -> List.of(
                RecipeView.builder(wheel, 150, 26).input(4, 5, new ItemStack(Items.WATER_BUCKET))
                        .text(26, 3, Component.translatable("lens.ls_aeolus.wheel1", (int) WaterWheelBE.FE_PER_TORQUE))
                        .text(26, 14, Component.translatable("lens.ls_aeolus.wheel2"), 0xFF606060).build()));
        ResourceLocation tidal = new ResourceLocation("ls_aeolus", "tidal");
        LensRegistry.register(cat(tidal, Content.TIDAL_GENERATOR, 322), level -> List.of(
                RecipeView.builder(tidal, 150, 26).input(4, 5, new ItemStack(Items.WATER_BUCKET))
                        .text(26, 3, Component.translatable("lens.ls_aeolus.tidal1", TidalGeneratorBE.PEAK))
                        .text(26, 14, Component.translatable("lens.ls_aeolus.tidal2"), 0xFF606060).build()));
        if (com.lodestar.core.test.SelfTest.enabled()) AeolusTests.register();
    }

    private static RecipeCategory cat(ResourceLocation id, Supplier<? extends ItemLike> block, int order) {
        return new RecipeCategory(id, Component.translatable("lens.category." + id.getNamespace() + "." + id.getPath()),
                () -> new ItemStack(block.get()), () -> List.of(new ItemStack(block.get())), order);
    }
}
