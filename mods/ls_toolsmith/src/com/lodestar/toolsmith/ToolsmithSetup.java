package com.lodestar.toolsmith;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import com.lodestar.core.registry.LSTab;
import com.lodestar.toolsmith.foundry.FoundryMenu;
import com.lodestar.toolsmith.foundry.FoundryRules;
import com.lodestar.toolsmith.station.PartBuilderMenu;
import com.lodestar.toolsmith.station.ToolStationMenu;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Hand-written registrations and recipe-viewer pages for Toolsmith. */
public final class ToolsmithSetup {
    private ToolsmithSetup() {}

    public static final List<String> MOLTEN_METALS = ToolsmithTables.MOLTEN_METALS;
    public static final Object[][] ALLOYS = ToolsmithTables.ALLOYS;
    public static BooleanSupplier SHIFT = () -> false;

    public static final TagKey<Block> FOUNDRY_WALLS = TagKey.create(Registries.BLOCK, new ResourceLocation("ls_toolsmith", "foundry_walls"));
    public static final TagKey<Block> FOUNDRY_FLOOR = TagKey.create(Registries.BLOCK, new ResourceLocation("ls_toolsmith", "foundry_floor"));

    public static final RegistryObject<MenuType<FoundryMenu>> FOUNDRY_MENU = Content.REG.menu("foundry", FoundryMenu::new);
    public static final RegistryObject<MenuType<PartBuilderMenu>> PART_BUILDER_MENU = Content.REG.menu("part_builder", PartBuilderMenu::new);
    public static final RegistryObject<MenuType<ToolStationMenu>> TOOL_STATION_MENU = Content.REG.menu("tool_station", ToolStationMenu::new);
    public static final RegistryObject<Codec<ToolTraitsModifier>> TOOL_TRAITS = Content.REG.lootModifiers().register("tool_traits", ToolTraitsModifier.CODEC);

    public static void init(IEventBus bus) {
        bus.addListener(ToolsmithSetup::tabs);
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent e) -> FoundryRules.clearTagCache());
        lens();
    }

    // ------------------------------------------------------------------ item lookups

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation("ls_toolsmith", path));
    }

    public static Item toolItem(ToolType t) {
        return item(t.id);
    }

    public static Item partItem(PartType p) {
        return item(p.id);
    }

    @Nullable
    public static Item castItem(String shape) {
        Item it = item(shape + "_cast");
        return it == Items.AIR ? null : it;
    }

    public static Item blankPattern() {
        return item("blank_pattern");
    }

    public static List<Item> toolItems() {
        List<Item> l = new ArrayList<>();
        for (ToolType t : ToolType.values()) l.add(toolItem(t));
        return l;
    }

    public static List<Item> partItems() {
        List<Item> l = new ArrayList<>();
        for (PartType p : PartType.values()) l.add(partItem(p));
        return l;
    }

    // ------------------------------------------------------------------ creative tab

    private static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() != LSTab.TOOLS) return;
        String[][] showcase = {{"wood", "wood", "string"}, {"stone", "wood", "leather"}, {"iron", "bone", "iron"}, {"bronze", "wood", "copper"},
                {"steel", "steel", "leather"}, {"cobalt", "cobalt_steel", "cobalt"}, {"tungsten", "steel", "platinum"}, {"netherite", "obsidian", "netherite"}};
        for (String[] s : showcase) {
            for (ToolType t : ToolType.values()) {
                List<ToolMaterial> mats = new ArrayList<>();
                for (String id : s) mats.add(Materials.get(id) == null ? Materials.FALLBACK : Materials.get(id));
                e.accept(ToolData.build(t, mats));
            }
        }
        for (ToolMaterial m : Materials.all()) {
            for (PartType p : PartType.values()) e.accept(PartItem.of(p, m));
        }
    }

    // ------------------------------------------------------------------ recipe viewer

    private static RecipeCategory cat(String id, java.util.function.Supplier<ItemStack> icon, int order) {
        return new RecipeCategory(new ResourceLocation("ls_toolsmith", id), Component.translatable("lens.category.ls_toolsmith." + id),
                icon, () -> List.of(icon.get()), order);
    }

    private static void lens() {
        ResourceLocation melting = new ResourceLocation("ls_toolsmith", "melting");
        LensRegistry.register(cat("melting", () -> new ItemStack(Content.FOUNDRY_CONTROLLER.get()), 130), level -> {
            List<RecipeView> out = new ArrayList<>();
            String[][] forms = {{"ingots", "144"}, {"nuggets", "16"}, {"storage_blocks", "1296"}, {"dusts", "144"}, {"plates", "144"},
                    {"gears", "576"}, {"rods", "72"}, {"ores", "288"}, {"raw_materials", "216"}};
            for (String metal : MOLTEN_METALS) {
                Fluid f = FoundryRules.molten(metal);
                if (f == Fluids.EMPTY) continue;
                for (String[] form : forms) {
                    Ingredient ing = Ingredient.of(TagKey.create(Registries.ITEM, new ResourceLocation("forge", form[0] + "/" + metal)));
                    if (ing.isEmpty()) continue;
                    out.add(RecipeView.builder(melting, 120, 30).input(4, 7, ing).arrow(28, 7)
                            .tank(60, 2, 40, 26, new FluidStack(f, Integer.parseInt(form[1])), 1296, true).build());
                }
            }
            return out;
        });
        ResourceLocation alloying = new ResourceLocation("ls_toolsmith", "alloying");
        LensRegistry.register(cat("alloying", () -> new ItemStack(Content.FOUNDRY_CONTROLLER.get()), 131), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (com.lodestar.toolsmith.foundry.FoundryRules.Alloy a : FoundryRules.alloys()) {
                if (a.output().getFluid() == Fluids.EMPTY) continue;
                RecipeView.Builder b = RecipeView.builder(alloying, 130, 34);
                int x = 2;
                for (FluidStack in : a.inputs()) {
                    b.tank(x, 2, 16, 30, new FluidStack(in.getFluid(), in.getAmount() * 36), 1296, false);
                    x += 20;
                }
                b.arrow(x + 4, 9).tank(x + 34, 2, 16, 30, new FluidStack(a.output().getFluid(), a.output().getAmount() * 36), 1296, true);
                out.add(b.build());
            }
            return out;
        });
        ResourceLocation table = new ResourceLocation("ls_toolsmith", "casting_table");
        LensRegistry.register(cat("casting_table", () -> new ItemStack(Content.CASTING_TABLE.get()), 132), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (String shape : FoundryRules.SHAPES.keySet()) {
                Item cast = castItem(shape);
                if (cast == null) continue;
                for (String metal : MOLTEN_METALS) {
                    Fluid f = FoundryRules.molten(metal);
                    if (f == Fluids.EMPTY) continue;
                    FoundryRules.CastResult r = FoundryRules.table(new ItemStack(cast), f);
                    if (r == null) continue;
                    out.add(RecipeView.builder(table, 120, 36).tank(2, 2, 16, 32, new FluidStack(f, r.amount()), Math.max(r.amount(), 144), false)
                            .catalyst(24, 10, List.of(new ItemStack(cast)), Component.translatable("lens.ls_core.not_consumed"))
                            .arrow(46, 10).output(78, 10, r.output()).build());
                }
            }
            return out;
        });
        ResourceLocation basin = new ResourceLocation("ls_toolsmith", "casting_basin");
        LensRegistry.register(cat("casting_basin", () -> new ItemStack(Content.CASTING_BASIN.get()), 133), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (String metal : MOLTEN_METALS) {
                Fluid f = FoundryRules.molten(metal);
                FoundryRules.CastResult r = f == Fluids.EMPTY ? null : FoundryRules.basin(f);
                if (r == null) continue;
                out.add(RecipeView.builder(basin, 110, 36).tank(2, 2, 16, 32, new FluidStack(f, r.amount()), r.amount(), false)
                        .arrow(26, 10).output(58, 10, r.output()).build());
            }
            return out;
        });
        ResourceLocation parts = new ResourceLocation("ls_toolsmith", "part_building");
        LensRegistry.register(cat("part_building", () -> new ItemStack(Content.PART_BUILDER.get()), 134), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (ToolMaterial m : Materials.all()) {
                Ingredient ing = m.ingredient();
                if (ing == null || ing.isEmpty()) continue;
                for (PartType p : PartType.values()) {
                    List<ItemStack> st = new ArrayList<>();
                    for (ItemStack s : ing.getItems()) st.add(s.copyWithCount(p.cost));
                    out.add(RecipeView.builder(parts, 110, 24).input(4, 4, st)
                            .catalyst(24, 4, List.of(new ItemStack(blankPattern())), Component.translatable("lens.ls_core.not_consumed"))
                            .arrow(46, 4).output(80, 4, PartItem.of(p, m)).build());
                }
            }
            return out;
        });
        ResourceLocation tools = new ResourceLocation("ls_toolsmith", "tool_building");
        LensRegistry.register(cat("tool_building", () -> new ItemStack(Content.TOOL_STATION.get()), 135), level -> {
            List<RecipeView> out = new ArrayList<>();
            ToolMaterial iron = Materials.get("iron"), wood = Materials.get("wood"), str = Materials.get("string");
            for (ToolType t : ToolType.values()) {
                RecipeView.Builder b = RecipeView.builder(tools, 130, 24);
                List<ToolMaterial> mats = new ArrayList<>();
                int x = 2;
                for (PartType p : t.parts) {
                    ToolMaterial m = p.role == PartType.Role.HEAD ? iron : p.role == PartType.Role.HANDLE ? wood : iron;
                    if (m == null) m = Materials.FALLBACK;
                    List<ItemStack> alts = new ArrayList<>();
                    alts.add(PartItem.of(p, m));
                    for (ToolMaterial any : Materials.all()) if (!any.id().equals(m.id())) alts.add(PartItem.of(p, any));
                    b.input(x, 4, alts);
                    mats.add(m);
                    x += 18;
                }
                b.arrow(x + 4, 4).output(x + 36, 4, ToolData.build(t, mats));
                if (t.large) b.text(2, 24 - 9, Component.translatable("lens.ls_toolsmith.forge_only"), 0xFF808080);
                out.add(b.build());
            }
            return out;
        });
        ResourceLocation mods = new ResourceLocation("ls_toolsmith", "modifiers");
        LensRegistry.register(cat("modifiers", () -> new ItemStack(Items.REDSTONE_BLOCK), 136), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (Modifiers.Def d : Modifiers.all()) {
                out.add(RecipeView.builder(mods, 150, 30).input(4, 6, d.item().get())
                        .text(26, 4, d.name()).text(26, 15, d.description(), 0xFF606060).build());
            }
            return out;
        });
    }
}
