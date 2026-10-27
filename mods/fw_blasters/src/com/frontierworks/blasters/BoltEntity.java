package com.frontierworks.blasters;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Gravity-free energy bolt. */
public class BoltEntity extends ThrowableItemProjectile {
    private float damage = 6.0f;
    private float explosion;

    public BoltEntity(EntityType<? extends BoltEntity> type, Level level) {
        super(type, level);
    }

    public BoltEntity(EntityType<? extends BoltEntity> type, LivingEntity owner, Level level, float damage, float explosion) {
        super(type, owner, level);
        this.damage = damage;
        this.explosion = explosion;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GLOWSTONE_DUST;
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 60) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity target = hit.getEntity();
        target.hurt(damageSources().thrown(this, getOwner()), damage);
        target.invulnerableTime = 0;
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide) return;
        if (explosion > 0) {
            level().explode(this, getX(), getY(), getZ(), explosion, Level.ExplosionInteraction.NONE);
        }
        discard();
    }
}
