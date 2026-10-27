package com.frontierworks.rocketry.client;

import com.frontierworks.core.net.CoreNet;
import com.frontierworks.rocketry.RocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class RocketryClient {
    private static int cooldown;

    private RocketryClient() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(RocketryClient::tick);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (cooldown > 0) cooldown--;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (mc.player.getVehicle() instanceof RocketEntity r && r.state() == RocketEntity.IDLE && mc.options.keyJump.isDown() && cooldown == 0) {
            CoreNet.send("fw_rocketry:launch", 0);
            cooldown = 20;
        }
    }
}
