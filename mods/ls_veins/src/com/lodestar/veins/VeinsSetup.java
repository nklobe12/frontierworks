package com.lodestar.veins;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Hand-written registrations for Deep Veins. */
public final class VeinsSetup {
    private VeinsSetup() {}

    public static final RegistryObject<Feature<VeinFeature.Config>> VEIN = Content.REG.features().register("vein", VeinFeature::new);

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent e) -> OreIndex.clear());
        ResourceLocation cat = new ResourceLocation("ls_veins", "veins");
        LensRegistry.register(new RecipeCategory(cat, Component.translatable("lens.category.ls_veins.veins"),
                () -> new ItemStack(Content.IRON_PROSPECTORS_PICK.get()),
                () -> List.of(new ItemStack(Content.COPPER_PROSPECTORS_PICK.get()), new ItemStack(Content.SEISMIC_SURVEYOR.get())), 900), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (VeinTable.Vein v : VeinTable.ALL) {
                RecipeView.Builder b = RecipeView.builder(cat, 150, 48);
                b.output(4, 4, oreStack(v.primary(), false));
                b.output(22, 4, oreStack(v.primary(), true));
                if (!v.secondary().isEmpty()) {
                    b.output(4, 24, oreStack(v.secondary(), false), v.secondaryChance());
                }
                b.text(46, 4, Component.translatable("vein.ls_veins." + v.id()));
                b.text(46, 15, Component.translatable("lens.ls_veins.depth", v.minY(), v.maxY()), 0xFF606060);
                b.text(46, 26, Component.translatable("lens.ls_veins.rarity", v.rarity()), 0xFF606060);
                if (!v.secondary().isEmpty()) {
                    b.text(46, 37, Component.translatable("lens.ls_veins.secondary", OreIndex.display(v.secondary()), Math.round(v.secondaryChance() * 100)), 0xFF606060);
                }
                out.add(b.build());
            }
            return out;
        });
    }

    private static ItemStack oreStack(String metal, boolean deep) {
        String ns = BuiltInRegistries.ITEM.containsKey(new ResourceLocation("ls_veins", metal + "_ore")) ? "ls_veins" : "minecraft";
        Item it = BuiltInRegistries.ITEM.get(new ResourceLocation(ns, (deep ? "deepslate_" : "") + metal + "_ore"));
        return it == Items.AIR ? new ItemStack(Items.STONE) : new ItemStack(it);
    }
}
