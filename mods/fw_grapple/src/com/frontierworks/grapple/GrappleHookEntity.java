package com.frontierworks.grapple;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Hook projectile: sticks to the first block it hits, then reels its owner in until they arrive, sneak or swap items. */
public class GrappleHookEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> ATTACHED = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);
    private double pull = 0.9;
    private int life;

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
        super(type, level);
    }

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, LivingEntity owner, Level level, double pull) {
        super(type, owner, level);
        this.pull = pull;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACHED, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Content.GRAPPLING_HOOK.get();
    }

    @Override
    protected float getGravity() {
        return 0.02f;
    }

    public boolean isAttached() {
        return entityData.get(ATTACHED);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        setDeltaMovement(Vec3.ZERO);
        setPos(hit.getLocation());
        setNoGravity(true);
        entityData.set(ATTACHED, true);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // hooks pass through entities harmlessly
    }

    @Override
    public void tick() {
        if (isAttached()) {
            // stay put; skip projectile movement
            baseTick();
        } else {
            super.tick();
        }
        if (level().isClientSide) return;
        Entity owner = getOwner();
        if (!(owner instanceof Player) || !owner.isAlive() || ++life > 600) {
            discard();
            return;
        }
        Player p = (Player) owner;
        boolean holding = p.getMainHandItem().getItem() instanceof GrappleItem || p.getOffhandItem().getItem() instanceof GrappleItem;
        if (!holding || p.isShiftKeyDown() || distanceTo(p) > 64) {
            discard();
            return;
        }
        if (isAttached()) {
            Vec3 to = position().subtract(p.position().add(0, p.getBbHeight() * 0.5, 0));
            double dist = to.length();
            if (dist < 1.6) {
                p.setDeltaMovement(p.getDeltaMovement().scale(0.2));
                p.hurtMarked = true;
                discard();
                return;
            }
            Vec3 v = to.normalize().scale(Math.min(pull, dist * 0.3));
            p.setDeltaMovement(v.add(0, 0.05, 0));
            p.hurtMarked = true;
            p.fallDistance = 0;
        }
    }
}
