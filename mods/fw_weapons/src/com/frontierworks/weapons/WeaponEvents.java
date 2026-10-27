package com.frontierworks.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.Map;
import java.util.WeakHashMap;

public final class WeaponEvents {
    private static final ThreadLocal<Boolean> SPLASHING = ThreadLocal.withInitial(() -> false);
    /** attack strength captured just before the swing resets it */
    private static final Map<Player, Float> STRENGTH = new WeakHashMap<>();

    private WeaponEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(WeaponEvents::onAttack);
        MinecraftForge.EVENT_BUS.addListener(WeaponEvents::onHurt);
    }

    private static void onAttack(AttackEntityEvent event) {
        STRENGTH.put(event.getEntity(), event.getEntity().getAttackStrengthScale(0.5f));
    }

    private static void onHurt(LivingHurtEvent event) {
        if (SPLASHING.get()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getSource().getDirectEntity() != attacker) return;
        if (!(attacker.getMainHandItem().getItem() instanceof WeaponItem weapon)) return;
        LivingEntity target = event.getEntity();
        if (weapon.kind == WeaponItem.Kind.DAGGER) {
            Vec3 look = target.getLookAngle();
            Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();
            if (look.x * toAttacker.x + look.z * toAttacker.z < -0.3) {
                event.setAmount(event.getAmount() * 2.0f);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                }
            }
        } else if (weapon.kind == WeaponItem.Kind.WARHAMMER) {
            float strength = attacker instanceof Player p ? STRENGTH.getOrDefault(p, 1.0f) : 1.0f;
            if (strength < 0.9f) return;
            float splash = event.getAmount() * 0.5f;
            SPLASHING.set(true);
            try {
                for (LivingEntity other : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5))) {
                    if (other == attacker || other == target || other.isAlliedTo(attacker)) continue;
                    other.hurt(event.getSource(), splash);
                    Vec3 push = other.position().subtract(target.position()).normalize().scale(0.6);
                    other.push(push.x, 0.25, push.z);
                }
            } finally {
                SPLASHING.set(false);
            }
            target.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 0.6f);
        }
    }
}
