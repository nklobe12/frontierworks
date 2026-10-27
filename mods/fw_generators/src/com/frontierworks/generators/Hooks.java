package com.frontierworks.generators;

import com.frontierworks.core.machine.FluidGeneratorBE;
import net.minecraft.world.level.material.Fluids;

public final class Hooks {
    private Hooks() {}

    public static void init() {
        Content.LAVA_FUELS.put("lava", new FluidGeneratorBE.Fuel(() -> Fluids.LAVA, 60, 20));
    }
}
