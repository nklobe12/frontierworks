package com.frontierworks.rocketry;

import com.frontierworks.core.net.CoreNet;

public final class RocketHooks {
    private RocketHooks() {}

    public static void init() {
        CoreNet.register("fw_rocketry:launch", (player, data) -> {
            if (player.getVehicle() instanceof RocketEntity rocket) rocket.tryLaunch(player);
        });
    }
}
