package com.frontierworks.wyvern;

import com.frontierworks.core.entity.BossMob;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Flying boss: circles, rains fireball volleys and dives at its target. */
public class SkyWyvern extends BossMob {
    private int volleyCooldown = 50;
    private int diveCooldown = 200;

    public SkyWyvern(EntityType<? extends SkyWyvern> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 280.0).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (t == null) return;
        double d2 = distanceToSqr(t);
        if (--volleyCooldown <= 0 && d2 > 25 && hasLineOfSight(t)) {
            volleyCooldown = phase() == 1 ? 30 : 50;
            for (int i = 0; i < 3; i++) {
                double dx = t.getX() - getX() + random.nextGaussian() * 1.5;
                double dy = t.getY(0.5) - getY(0.5);
                double dz = t.getZ() - getZ() + random.nextGaussian() * 1.5;
                SmallFireball fb = new SmallFireball(level(), this, dx, dy, dz);
                fb.setPos(getX() + getLookAngle().x * 2, getY(0.5), getZ() + getLookAngle().z * 2);
                level().addFreshEntity(fb);
            }
            playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.2f, 1.2f);
        }
        if (--diveCooldown <= 0) {
            diveCooldown = phase() == 1 ? 120 : 200;
            Vec3 dir = t.position().add(0, 1, 0).subtract(position()).normalize();
            setDeltaMovement(dir.scale(1.6));
            playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 1.4f);
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f;
    }
}
