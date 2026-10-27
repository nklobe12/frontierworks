package com.lodestar.machina;

import com.lodestar.core.recipe.MachineRecipeKind;
import com.lodestar.core.recipe.display.LensRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.function.Supplier;

/** Hand-written setup for Machina: recipe viewer categories for every machine. */
public final class MachinaSetup {
    private MachinaSetup() {}

    public static void init(IEventBus bus) {
        lens(Content.PULVERIZING, Content.PULVERIZER, 200);
        lens(Content.INDUCTION_SMELTING, Content.INDUCTION_SMELTER, 201);
        lens(Content.SAWING, Content.SAWMILL, 202);
        lens(Content.INSOLATING, Content.INSOLATOR, 203);
        lens(Content.CENTRIFUGING, Content.CENTRIFUGE, 204);
        lens(Content.MELTING, Content.MAGMA_CRUCIBLE, 205);
        lens(Content.CHILLING, Content.BLAST_CHILLER, 206);
        lens(Content.PRESSING, Content.COMPACTOR, 207);
        lens(Content.REFINING, Content.REFINERY, 208);
        lens(Content.IMBUING, Content.FLUID_BOTTLER, 209);
        lens(Content.PYROLYSIS, Content.PYROLYZER, 210);
        LensRegistry.addCatalyst(new ResourceLocation("minecraft", "smelting"), () -> new ItemStack(Content.ELECTRIC_FURNACE.get()));
    }

    private static void lens(MachineRecipeKind kind, Supplier<? extends ItemLike> machine, int order) {
        LensRegistry.machine(kind, () -> new ItemStack(machine.get()), order);
    }
}
