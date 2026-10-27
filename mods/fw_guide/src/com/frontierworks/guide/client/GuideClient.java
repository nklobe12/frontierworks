package com.frontierworks.guide.client;

import net.minecraft.client.Minecraft;

public final class GuideClient {
    private GuideClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen(GuideData.load()));
    }
}
