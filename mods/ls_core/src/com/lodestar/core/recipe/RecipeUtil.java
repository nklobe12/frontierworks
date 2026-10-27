package com.lodestar.core.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Small helpers for reading vanilla recipes. */
public final class RecipeUtil {
    private RecipeUtil() {}

    private static final Map<Class<?>, List<Field>> SMITHING_FIELDS = new ConcurrentHashMap<>();

    /**
     * Template, base and addition of a smithing recipe (vanilla keeps them in package-private fields and reports no
     * ingredients). Empty when the recipe is not a vanilla smithing recipe.
     */
    public static List<Ingredient> smithingInputs(Recipe<?> r) {
        if (!(r instanceof SmithingTransformRecipe) && !(r instanceof SmithingTrimRecipe)) return List.of();
        List<Field> fields = SMITHING_FIELDS.computeIfAbsent(r.getClass(), c -> {
            List<Field> l = new ArrayList<>();
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == Ingredient.class && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    l.add(f);
                }
            }
            return l;
        });
        List<Ingredient> out = new ArrayList<>();
        try {
            for (Field f : fields) out.add((Ingredient) f.get(r));
        } catch (IllegalAccessException e) {
            return List.of();
        }
        return out.size() == 3 ? out : List.of();
    }
}
