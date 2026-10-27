package com.frontierworks.petro;

import com.frontierworks.core.machine.FluidGeneratorBE;
import com.frontierworks.core.util.FW;

public final class Hooks {
    private Hooks() {}

    public static void registerFuels() {
        Content.FUELS.put("diesel", new FluidGeneratorBE.Fuel(() -> Content.DIESEL.get(), 80, 20));
        Content.FUELS.put("biodiesel", new FluidGeneratorBE.Fuel(() -> Content.BIODIESEL.get(), 60, 20));
        Content.FUELS.put("rocket_fuel", new FluidGeneratorBE.Fuel(() -> FW.fluid("fw_rocketry:rocket_fuel"), 160, 25));
    }
}
