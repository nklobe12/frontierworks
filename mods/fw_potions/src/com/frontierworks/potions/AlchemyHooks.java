package com.frontierworks.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;

public final class AlchemyHooks {
    private AlchemyHooks() {}

    public static void init(IEventBus bus) {
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Brewing::register));
        MinecraftForge.EVENT_BUS.addListener(AlchemyHooks::onLivingTick);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        if (e.tickCount % 20 == 0 && e.hasEffect(Content.PURITY.get())) {
            List<MobEffect> bad = new ArrayList<>();
            for (MobEffectInstance inst : e.getActiveEffects()) {
                if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) bad.add(inst.getEffect());
            }
            for (MobEffect b : bad) e.removeEffect(b);
        }
        MobEffectInstance mag = e.getEffect(Content.MAGNETISM.get());
        if (mag != null) {
            double r = 6 + 2 * mag.getAmplifier();
            for (ItemEntity item : e.level().getEntitiesOfClass(ItemEntity.class, e.getBoundingBox().inflate(r))) {
                if (item.hasPickUpDelay()) continue;
                Vec3 pull = e.position().add(0, 0.5, 0).subtract(item.position());
                double d = pull.length();
                if (d > 0.5) item.setDeltaMovement(item.getDeltaMovement().scale(0.7).add(pull.scale(0.12 / d)));
            }
        }
    }
}
