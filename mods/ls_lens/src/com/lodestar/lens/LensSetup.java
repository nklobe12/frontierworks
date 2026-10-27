package com.lodestar.lens;

import com.lodestar.core.net.CoreNet;
import net.minecraftforge.eventbus.api.IEventBus;

public final class LensSetup {
    private LensSetup() {}

    public static void init(IEventBus bus) {
        VanillaCategories.register();
        CoreNet.registerTag("ls_lens:transfer", Transfer::handle);
    }
}
