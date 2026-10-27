package com.lodestar.veins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class VeinsClient {
    private VeinsClient() {}

    public static void openReport(ItemStack stack) {
        Minecraft.getInstance().setScreen(new SurveyReportScreen(stack.getOrCreateTag().copy()));
    }
}
