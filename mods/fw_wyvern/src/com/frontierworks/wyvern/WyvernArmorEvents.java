package com.frontierworks.wyvern;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class WyvernArmorEvents {
    private WyvernArmorEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(WyvernArmorEvents::onTick);
    }

    private static boolean fullSet(Player p) {
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (!(st.getItem() instanceof ArmorItem a) || a.getMaterial() != Content.WYVERN_MAT) return false;
        }
        return true;
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (!fullSet(p)) return;
        if (!p.level().isClientSide && p.tickCount % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
        }
        Vec3 v = p.getDeltaMovement();
        if (p.isShiftKeyDown() && !p.onGround() && v.y < -0.12 && !p.getAbilities().flying) {
            p.setDeltaMovement(v.x * 1.02, -0.12, v.z * 1.02);
            p.fallDistance = 0;
        }
    }
}
