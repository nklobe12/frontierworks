package com.frontierworks.steam;

import com.frontierworks.core.machine.FluidGeneratorBE;

public final class Hooks {
    private Hooks() {}

    public static void init() {
        Content.ENGINE_FUELS.put("steam", new FluidGeneratorBE.Fuel(() -> Content.STEAM.get(), 40, 400));
        Content.TURBINE_FUELS.put("steam", new FluidGeneratorBE.Fuel(() -> Content.STEAM.get(), 160, 1200));
    }
}
