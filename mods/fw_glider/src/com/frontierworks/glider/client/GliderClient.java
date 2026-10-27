package com.frontierworks.glider.client;

import com.frontierworks.core.net.CoreNet;
import com.frontierworks.glider.GliderItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class GliderClient {
    private GliderClient() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(GliderClient::tick);
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.isPaused() || p.onGround() || p.isInWater() || p.getAbilities().flying || p.isShiftKeyDown() || p.isPassenger()) return;
        ItemStack st = GliderItem.held(p);
        if (st.isEmpty()) return;
        GliderItem g = (GliderItem) st.getItem();
        Vec3 v = p.getDeltaMovement();
        if (v.y >= -0.05) return;
        Vec3 look = p.getLookAngle();
        double hx = v.x * 0.92 + look.x * g.speed;
        double hz = v.z * 0.92 + look.z * g.speed;
        double max = 0.9;
        double h = Math.sqrt(hx * hx + hz * hz);
        if (h > max) {
            hx = hx / h * max;
            hz = hz / h * max;
        }
        p.setDeltaMovement(hx, Math.max(v.y, -g.sink), hz);
        p.fallDistance = 0;
        CoreNet.send("fw_glider:glide", 0);
    }
}
