package com.lodestar.lens.client;

import com.lodestar.core.client.LensBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class LensClient {
    private static final String CAT = "key.categories.ls_lens";
    public static final KeyMapping RECIPES = new KeyMapping("key.ls_lens.recipes", KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping USES = new KeyMapping("key.ls_lens.uses", KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CAT);
    public static final KeyMapping BOOKMARK = new KeyMapping("key.ls_lens.bookmark", KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_A, CAT);
    public static final KeyMapping TOGGLE = new KeyMapping("key.ls_lens.toggle", KeyConflictContext.GUI, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CAT);
    public static final KeyMapping FOCUS = new KeyMapping("key.ls_lens.focus", KeyConflictContext.GUI, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CAT);

    private LensClient() {}

    /** Key test that respects modifiers (Ctrl+O must not fire on a plain O). */
    public static boolean is(KeyMapping k, int key, int scan) {
        return k.isActiveAndMatches(InputConstants.getKey(key, scan));
    }

    public static void init(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(RECIPES);
            e.register(USES);
            e.register(BOOKMARK);
            e.register(TOGGLE);
            e.register(FOCUS);
        });
        MinecraftForge.EVENT_BUS.addListener(Overlay::onInit);
        MinecraftForge.EVENT_BUS.addListener(Overlay::onRender);
        MinecraftForge.EVENT_BUS.addListener(Overlay::onMouse);
        MinecraftForge.EVENT_BUS.addListener(Overlay::onScroll);
        MinecraftForge.EVENT_BUS.addListener(Overlay::onKey);
        MinecraftForge.EVENT_BUS.addListener((RecipesUpdatedEvent e) -> RecipeIndex.clear());
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent e) -> {
            RecipeIndex.clear();
            ItemList.clear();
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            RecipeIndex.clear();
            ItemList.clear();
        });
        LensBridge.recipes = s -> LensScreen.open(RecipeIndex.Mode.RECIPES, s, FluidStack.EMPTY, null);
        LensBridge.uses = s -> LensScreen.open(RecipeIndex.Mode.USES, s, FluidStack.EMPTY, null);
        LensBridge.fluidRecipes = f -> LensScreen.open(RecipeIndex.Mode.RECIPES, ItemStack.EMPTY, f, null);
        LensBridge.fluidUses = f -> LensScreen.open(RecipeIndex.Mode.USES, ItemStack.EMPTY, f, null);
        LensBridge.category = c -> LensScreen.open(RecipeIndex.Mode.CATEGORY, ItemStack.EMPTY, FluidStack.EMPTY, c);
        LensBridge.present = true;
    }
}
