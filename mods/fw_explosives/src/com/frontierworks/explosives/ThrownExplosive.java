package com.frontierworks.explosives;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Every thrown explosive: dynamite bounces until its fuse runs out, grenades detonate on impact. */
public class ThrownExplosive extends ThrowableItemProjectile {
    public enum Kind {
        DYNAMITE(50, true), BUNDLE(60, true), MINING(30, true), FRAG(0, false), FIRE(0, false), SMOKE(0, false);

        final int fuse;
        final boolean bounce;

        Kind(int fuse, boolean bounce) {
            this.fuse = fuse;
            this.bounce = bounce;
        }
    }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ThrownExplosive.class, EntityDataSerializers.INT);
    private int fuse = -1;
    private boolean landed;

    public ThrownExplosive(EntityType<? extends ThrownExplosive> type, Level level) {
        super(type, level);
    }

    public ThrownExplosive(EntityType<? extends ThrownExplosive> type, LivingEntity owner, Level level, Kind kind) {
        super(type, owner, level);
        entityData.set(KIND, kind.ordinal());
        fuse = kind.fuse;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(KIND, 0);
    }

    public Kind kind() {
        int k = entityData.get(KIND);
        return Kind.values()[Math.max(0, Math.min(k, Kind.values().length - 1))];
    }

    @Override
    protected Item getDefaultItem() {
        return Content.DYNAMITE.get();
    }

    @Override
    public void tick() {
        if (landed) {
            setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
        }
        super.tick();
        Kind k = kind();
        if (level().isClientSide) {
            if (k.bounce) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.03, 0);
            return;
        }
        if (k.bounce && --fuse <= 0) {
            detonate();
        } else if (tickCount > 400) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (kind().bounce) {
            Vec3 v = getDeltaMovement();
            switch (hit.getDirection().getAxis()) {
                case X -> setDeltaMovement(-v.x * 0.35, v.y * 0.6, v.z * 0.6);
                case Y -> setDeltaMovement(v.x * 0.5, -v.y * 0.3, v.z * 0.5);
                default -> setDeltaMovement(v.x * 0.6, v.y * 0.6, -v.z * 0.35);
            }
            if (hit.getDirection() == net.minecraft.core.Direction.UP && Math.abs(v.y) < 0.15) {
                setDeltaMovement(Vec3.ZERO);
                landed = true;
            }
        } else if (!level().isClientSide) {
            detonate();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!kind().bounce && !level().isClientSide) detonate();
        else setDeltaMovement(getDeltaMovement().scale(-0.2));
    }

    private void detonate() {
        if (isRemoved()) return;
        Level lvl = level();
        double x = getX(), y = getY(), z = getZ();
        switch (kind()) {
            case DYNAMITE -> lvl.explode(this, x, y, z, 2.5f, Level.ExplosionInteraction.TNT);
            case BUNDLE -> lvl.explode(this, x, y, z, 4.5f, Level.ExplosionInteraction.TNT);
            case MINING -> mine(lvl, blockPosition());
            case FRAG -> {
                lvl.explode(this, x, y, z, 1.8f, Level.ExplosionInteraction.NONE);
                for (LivingEntity e : lvl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4))) {
                    e.hurt(damageSources().explosion(this, getOwner()), 6.0f);
                }
            }
            case FIRE -> {
                lvl.explode(this, x, y, z, 1.0f, Level.ExplosionInteraction.NONE);
                BlockPos c = blockPosition();
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -1, -3), c.offset(3, 2, 3))) {
                    if (random.nextFloat() < 0.6f && lvl.isEmptyBlock(p)) {
                        BlockState fire = BaseFireBlock.getState(lvl, p);
                        if (fire.canSurvive(lvl, p)) lvl.setBlockAndUpdate(p, fire);
                    }
                }
                for (LivingEntity e : lvl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4))) e.setSecondsOnFire(6);
            }
            case SMOKE -> {
                if (lvl instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 120, 2.5, 1.5, 2.5, 0.01);
                for (LivingEntity e : lvl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5))) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                }
            }
        }
        discard();
    }

    private void mine(Level lvl, BlockPos c) {
        lvl.explode(this, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 0.5f, Level.ExplosionInteraction.NONE);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -2, -2), c.offset(2, 2, 2))) {
            BlockState st = lvl.getBlockState(p);
            float h = st.getDestroySpeed(lvl, p);
            if (st.isAir() || h < 0 || h > 30 || lvl.getBlockEntity(p) != null || !st.getFluidState().isEmpty()) continue;
            lvl.destroyBlock(p, true, getOwner());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", entityData.get(KIND));
        tag.putInt("Fuse", fuse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
        fuse = tag.getInt("Fuse");
    }
}
