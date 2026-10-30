package com.lodestar.jetpacks;

import com.lodestar.core.energy.ItemEnergy;
import com.lodestar.core.net.CoreNet;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import com.lodestar.core.test.SelfTest;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class JetpacksSetup {
    public static final RegistryObject<RecipeSerializer<JetpackUpgradeRecipe>> UPGRADE_SERIALIZER = Content.REG.recipeSerializers().register("jetpack_upgrade",
            () -> new SimpleCraftingRecipeSerializer<>(JetpackUpgradeRecipe::new));
    public static final RegistryObject<RecipeSerializer<TieredJetpackRecipe>> TIERED_SERIALIZER = Content.REG.recipeSerializers().register("tiered_jetpack",
            TieredJetpackRecipe.Serializer::new);

    private JetpacksSetup() {}

    public static void init(IEventBus bus) {
        CoreNet.register("ls_jetpacks:input", JetpackLogic::setInput);
        CoreNet.register("ls_jetpacks:toggle", JetpackLogic::toggle);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer sp) JetpackLogic.serverTick(sp);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> JetpackLogic.forget(e.getEntity().getUUID()));
        lens();
        if (SelfTest.enabled()) JetpacksTests.register();
    }

    public static JetpackItem jetpack(JetpackTier t) {
        return (JetpackItem) ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_jetpacks", t.itemName()));
    }

    /** A full jetpack of a tier, for Lens and tests. */
    public static ItemStack full(JetpackTier t, Fluid fuel) {
        ItemStack s = new ItemStack(jetpack(t));
        if (t.fuel) FluidUtil.getFluidHandler(s).ifPresent(h -> h.fill(new FluidStack(fuel, t.capacity), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        else ItemEnergy.set(s, t.capacity);
        return s;
    }

    private static void lens() {
        ResourceLocation fuels = new ResourceLocation("ls_jetpacks", "jet_fuels");
        LensRegistry.register(new RecipeCategory(fuels, Component.translatable("lens.category.ls_jetpacks.jet_fuels"),
                () -> new ItemStack(jetpack(JetpackTier.STEEL)), () -> List.of(new ItemStack(jetpack(JetpackTier.BRONZE)), new ItemStack(jetpack(JetpackTier.STEEL))), 400),
                level -> {
                    List<RecipeView> out = new ArrayList<>();
                    for (Fluid f : JetFuels.all()) {
                        double g = JetFuels.grade(f);
                        double secs = 1000.0 / (JetpackTier.BRONZE.use / g) / 20.0;
                        out.add(RecipeView.builder(fuels, 150, 44).tank(4, 2, 16, 40, new FluidStack(f, 1000), 1000, false)
                                .text(26, 6, Component.translatable("lens.ls_jetpacks.grade", String.format("%.2f", g)), 0xFF404040)
                                .text(26, 18, Component.translatable("lens.ls_jetpacks.flight", String.format("%.0f", secs)), 0xFF606060).build());
                    }
                    return out;
                });
        ResourceLocation ups = new ResourceLocation("ls_jetpacks", "jetpack_upgrades");
        LensRegistry.register(new RecipeCategory(ups, Component.translatable("lens.category.ls_jetpacks.jetpack_upgrades"),
                () -> new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_jetpacks", "thrust_booster"))),
                () -> List.of(new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE)), 401),
                level -> {
                    List<RecipeView> out = new ArrayList<>();
                    for (JetpackItem.Mod m : JetpackItem.Mod.values()) {
                        ItemStack up = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_jetpacks", m.item)));
                        List<ItemStack> packs = new ArrayList<>(), results = new ArrayList<>();
                        for (JetpackTier t : JetpackTier.values()) {
                            if (m == JetpackItem.Mod.STABILIZER && t.hover) continue;
                            packs.add(new ItemStack(jetpack(t)));
                            ItemStack r = new ItemStack(jetpack(t));
                            JetpackItem.setLevel(r, m, 1);
                            results.add(r);
                        }
                        out.add(RecipeView.builder(ups, 120, 40).input(4, 4, packs).input(22, 4, up).arrow(48, 3).outputs(84, 4, results)
                                .text(4, 28, Component.translatable("lens.ls_jetpacks.max", m.max), 0xFF606060).build());
                    }
                    return out;
                });
    }
}
