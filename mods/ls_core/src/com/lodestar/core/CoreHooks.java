package com.lodestar.core;

import com.lodestar.core.net.CoreNet;
import com.lodestar.core.test.SelfTest;
import net.minecraftforge.eventbus.api.IEventBus;

/** Hand-written core initialisation, called from the generated CoreMod constructor. */
public final class CoreHooks {
    private CoreHooks() {}

    public static void init(IEventBus bus) {
        CoreRegistry.init();
        CoreNet.init();
        SelfTest.init();
    }
}
