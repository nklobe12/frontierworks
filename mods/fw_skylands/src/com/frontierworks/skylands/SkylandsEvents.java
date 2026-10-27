package com.frontierworks.skylands;

import com.frontierworks.core.util.TeleportUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class SkylandsEvents {
    private SkylandsEvents() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(SkylandsEvents::onTick);
    }

    private static boolean carriesWings(Player p) {
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof SkyWingsItem) return true;
        return p.getOffhandItem().getItem() instanceof SkyWingsItem;
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        Vec3 v = p.getDeltaMovement();
        if (p.isShiftKeyDown() && !p.onGround() && v.y < -0.15 && !p.getAbilities().flying && p.tickCount % 2 == 0 && carriesWings(p)) {
            p.setDeltaMovement(v.x, -0.15, v.z);
            p.resetFallDistance();
        }
        if (p.level().isClientSide || !(p instanceof ServerPlayer sp)) return;
        if (sp.getY() < -24 && sp.level().dimension().location().toString().equals("fw_skylands:skylands")) {
            ServerLevel ow = sp.server.overworld();
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
            sp.resetFallDistance();
            TeleportUtil.teleport(sp, ow, sp.getX(), 320, sp.getZ(), sp.getYRot(), sp.getXRot());
        }
    }
}
