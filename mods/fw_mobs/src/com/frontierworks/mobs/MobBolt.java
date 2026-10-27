package com.frontierworks.mobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Projectile fired by drones (sparks) and frost wraiths (frost bolts). */
public class MobBolt extends ThrowableItemProjectile {
    private float damage = 4.0f;
    private boolean frost;

    public MobBolt(EntityType<? extends MobBolt> type, Level level) {
        super(type, level);
    }

    public MobBolt(LivingEntity owner, Level level, float damage, boolean frost) {
        super(Content.MOB_BOLT.get(), owner, level);
        this.damage = damage;
        this.frost = frost;
        setItem(new net.minecraft.world.item.ItemStack(frost ? Content.FROST_BOLT.get() : Content.SPARK_BOLT.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return Content.SPARK_BOLT.get();
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(frost ? ParticleTypes.SNOWFLAKE : ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 80) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() == getOwner()) return;
        hit.getEntity().hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity le ? le : null), damage);
        if (frost && hit.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 80));
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) discard();
    }
}
