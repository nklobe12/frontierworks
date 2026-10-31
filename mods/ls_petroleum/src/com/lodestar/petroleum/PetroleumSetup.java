package com.lodestar.petroleum;

import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.multiblock.MultiblockPattern;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Hand-written registrations for Petroleum: the oil seep feature, Lens pages, multiblock previews and self-tests. */
public final class PetroleumSetup {
    private PetroleumSetup() {}

    public static final RegistryObject<Feature<OilSeepFeature.Config>> OIL_SEEP = Content.REG.features().register("oil_seep", OilSeepFeature::new);

    /** Cracker: two input tanks, two by-product slots and two output tanks. */
    public static final MachineLayout CRACKER_LAYOUT = MachineLayout.builder()
            .energy(8, 17, 38).slot(9, 59, MachineLayout.Role.BATTERY)
            .tank(26, 17, 16, 52, 8000, true, false).tank(46, 17, 16, 52, 8000, true, false).arrow(66, 34)
            .slot(94, 26, MachineLayout.Role.OUTPUT).slot(94, 44, MachineLayout.Role.OUTPUT).tank(116, 17, 16, 52, 8000, false, true).tank(134, 17, 16, 52, 8000, false, true)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .build();

    public static void init(IEventBus bus) {
        LensRegistry.machine(Content.CRACKING, () -> new ItemStack(Content.CRACKER.get()), 232);
        LensRegistry.machine(Content.POLYMERIZING, () -> new ItemStack(Content.POLYMERIZER.get()), 233);
        MultiblockPattern.registerPreview("ls_petroleum:pumpjack", PumpjackBE::pattern, () -> Content.PUMPJACK.get());
        MultiblockPattern.registerPreview("ls_petroleum:distillation", DistillationBE::previewPattern, () -> Content.DISTILLATION_COLUMN.get());

        ResourceLocation pump = new ResourceLocation("ls_petroleum", "pumpjack");
        LensRegistry.register(new RecipeCategory(pump, Component.translatable("lens.category.ls_petroleum.pumpjack"),
                () -> new ItemStack(Content.PUMPJACK.get()),
                () -> List.of(new ItemStack(Content.PUMPJACK.get()), new ItemStack(Content.WELLHEAD.get()), new ItemStack(Content.GEOPHONE.get())), 230),
                level -> List.of(RecipeView.builder(pump, 150, 52)
                        .catalyst(4, 4, List.of(new ItemStack(Content.GEOPHONE.get())), Component.translatable("lens.ls_petroleum.survey"))
                        .catalyst(4, 24, List.of(new ItemStack(Content.WELLHEAD.get())), Component.translatable("lens.ls_petroleum.reservoir"))
                        .arrow(26, 14)
                        .tank(54, 2, 16, 40, new FluidStack(Content.CRUDE_OIL.get(), PumpjackBE.STROKE_MB), 1000, true)
                        .text(76, 4, Component.translatable("lens.ls_petroleum.reservoir"))
                        .text(76, 16, Component.translatable("lens.ls_petroleum.stroke", PumpjackBE.STROKE_MB, PumpjackBE.BASE_FE), 0xFF606060)
                        .build()));

        ResourceLocation dist = new ResourceLocation("ls_petroleum", "distillation");
        LensRegistry.register(new RecipeCategory(dist, Component.translatable("lens.category.ls_petroleum.distillation"),
                () -> new ItemStack(Content.DISTILLATION_COLUMN.get()),
                () -> List.of(new ItemStack(Content.DISTILLATION_COLUMN.get()), new ItemStack(Content.DISTILLATION_TRAY.get())), 231),
                level -> {
                    List<RecipeView> out = new ArrayList<>();
                    for (int n = DistillationBE.MAX_TRAYS; n >= 2; n--) {
                        RecipeView.Builder b = RecipeView.builder(dist, 150, 62);
                        b.tank(2, 2, 16, 40, new FluidStack(Content.CRUDE_OIL.get(), DistillationBE.BATCH), 100, false);
                        b.catalyst(22, 14, List.of(new ItemStack(Content.DISTILLATION_TRAY.get(), n)),
                                Component.translatable("lens.ls_petroleum.trays", n));
                        b.arrow(42, 14);
                        for (int i = 0; i < n; i++) {
                            b.tank(70 + i * 16, 2 + (4 - i) * 2, 12, 40 - (4 - i) * 2,
                                    new FluidStack(DistillationBE.FRACTIONS[i].get(), DistillationBE.YIELD[i]), 30, true);
                        }
                        b.text(2, 46, Component.translatable("lens.ls_petroleum.trays", n));
                        if (n < DistillationBE.MAX_TRAYS) b.text(40, 46, Component.translatable("lens.ls_petroleum.flared"), 0xFF906030);
                        out.add(b.build());
                    }
                    return out;
                });
        if (com.lodestar.core.test.SelfTest.enabled()) PetroleumTests.register();
    }
}
