package com.frontierworks.jetpack.client;

import com.frontierworks.core.energy.ItemEnergy;
import com.frontierworks.core.net.CoreNet;
import com.frontierworks.jetpack.JetpackItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class JetpackClient {
    public static final KeyMapping HOVER = new KeyMapping("key.fw_jetpack.hover", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.frontierworks");

    private JetpackClient() {}

    public static void init(IEventBus bus) {
        bus.addListener(JetpackClient::keys);
        MinecraftForge.EVENT_BUS.addListener(JetpackClient::tick);
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.register(HOVER);
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.isPaused()) return;
        while (HOVER.consumeClick()) {
            if (p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof JetpackItem) CoreNet.send("fw_jetpack:hover", 0);
        }
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof JetpackItem) || p.isSpectator() || p.getAbilities().flying || p.isPassenger()) return;
        JetpackItem jp = (JetpackItem) chest.getItem();
        if (!ItemEnergy.has(chest, jp.cost)) return;
        Vec3 v = p.getDeltaMovement();
        boolean jump = mc.options.keyJump.isDown();
        if (jump && mc.screen == null) {
            double up = Math.min(v.y + jp.accel, jp.maxUp);
            double hx = v.x, hz = v.z;
            if (p.isSprinting() && jp.boost > 0) {
                Vec3 look = p.getLookAngle();
                hx += look.x * jp.boost;
                hz += look.z * jp.boost;
            }
            p.setDeltaMovement(hx, up, hz);
            p.fallDistance = 0;
            CoreNet.send("fw_jetpack:thrust", 1);
            for (int i = 0; i < 2; i++) {
                p.level().addParticle(ParticleTypes.FLAME, p.getX() + (p.getRandom().nextDouble() - 0.5) * 0.4, p.getY() + 0.6,
                        p.getZ() + (p.getRandom().nextDouble() - 0.5) * 0.4, 0, -0.2, 0);
            }
            p.level().addParticle(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.4, p.getZ(), 0, -0.1, 0);
        } else if (JetpackItem.hover(chest) && !p.onGround() && !p.isShiftKeyDown() && v.y < -0.05) {
            p.setDeltaMovement(v.x, Math.max(v.y, -0.06), v.z);
            p.fallDistance = 0;
            CoreNet.send("fw_jetpack:thrust", 2);
            if (p.tickCount % 3 == 0) p.level().addParticle(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 0, -0.1, 0);
        }
    }
}
