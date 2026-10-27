package com.frontierworks.depths;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class DepthsEvents {
    private DepthsEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DepthsEvents::onTick);
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        Player p = event.player;
        if (event.phase != TickEvent.Phase.END || p.level().isClientSide || p.tickCount % 40 != 0) return;
        if (!p.level().dimension().location().toString().equals("fw_depths:depths")) return;
        if (p.getOffhandItem().getItem() instanceof AbyssalLensItem || p.getMainHandItem().getItem() instanceof AbyssalLensItem) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        }
    }
}
