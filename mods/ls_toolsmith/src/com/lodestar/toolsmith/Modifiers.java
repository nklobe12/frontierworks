package com.lodestar.toolsmith;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Tool modifiers, added in the tool station by combining a tool with the modifier's item. Each level uses a
 * modifier slot; tools have three slots plus one per tool level.
 */
public final class Modifiers {
    public record Def(String id, Supplier<Ingredient> item, int maxLevel, int slots, @Nullable String excludes, @Nullable String requires,
                      boolean largeOnly) {
        public Component name() {
            return Component.translatable("modifier.ls_toolsmith." + id);
        }

        public Component description() {
            return Component.translatable("modifier.ls_toolsmith." + id + ".desc");
        }
    }

    private static final Map<String, Def> ALL = new LinkedHashMap<>();

    static {
        add(new Def("haste", () -> Ingredient.of(Items.REDSTONE_BLOCK), 5, 1, null, null, false));
        add(new Def("sharpness", () -> Ingredient.of(Items.QUARTZ_BLOCK), 5, 1, null, null, false));
        add(new Def("luck", () -> Ingredient.of(Items.LAPIS_BLOCK), 3, 1, "silky", null, false));
        add(new Def("silky", () -> Ingredient.of(Content.SILKY_CLOTH.get()), 1, 1, "luck", null, false));
        add(new Def("reinforced", () -> Ingredient.of(Items.OBSIDIAN), 5, 1, null, null, false));
        add(new Def("diamond", () -> Ingredient.of(Items.DIAMOND), 1, 1, null, null, false));
        add(new Def("emerald", () -> Ingredient.of(Items.EMERALD), 1, 1, null, null, false));
        add(new Def("netherite", () -> Ingredient.of(Items.NETHERITE_INGOT), 1, 1, null, "diamond", false));
        add(new Def("beheading", () -> Ingredient.of(Items.WITHER_SKELETON_SKULL), 3, 1, null, null, false));
        add(new Def("knockback", () -> Ingredient.of(Items.PISTON), 3, 1, null, null, false));
        add(new Def("fiery", () -> Ingredient.of(Items.BLAZE_POWDER), 2, 1, null, null, false));
        add(new Def("expanded", () -> Ingredient.of(Content.EXPANDER.get()), 1, 2, null, null, true));
        add(new Def("smite", () -> Ingredient.of(ItemTags.create(new ResourceLocation("forge", "ingots/silver"))), 5, 1, null, null, false));
    }

    private Modifiers() {}

    private static void add(Def d) {
        ALL.put(d.id, d);
    }

    @Nullable
    public static Def get(String id) {
        return ALL.get(id);
    }

    public static List<Def> all() {
        return List.copyOf(ALL.values());
    }

    @Nullable
    public static Def forItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Def d : ALL.values()) if (d.item().get().test(stack)) return d;
        return null;
    }

    /** Why a modifier can't be added, or null if it can. */
    @Nullable
    public static Component check(ItemStack tool, Def d) {
        if (!(tool.getItem() instanceof ModularToolItem mt)) return Component.translatable("msg.ls_toolsmith.not_tool");
        if (d.largeOnly() && !mt.type.large) return Component.translatable("msg.ls_toolsmith.large_only");
        int cur = ToolData.modifier(tool, d.id());
        if (cur >= d.maxLevel()) return Component.translatable("msg.ls_toolsmith.max_level");
        if (d.excludes() != null && ToolData.modifier(tool, d.excludes()) > 0) return Component.translatable("msg.ls_toolsmith.excludes", get(d.excludes()).name());
        if (d.requires() != null && ToolData.modifier(tool, d.requires()) == 0) return Component.translatable("msg.ls_toolsmith.requires", get(d.requires()).name());
        if (ToolData.freeSlots(tool) < d.slots()) return Component.translatable("msg.ls_toolsmith.no_slots");
        return null;
    }
}
