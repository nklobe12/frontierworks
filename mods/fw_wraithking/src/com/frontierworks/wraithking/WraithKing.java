package com.frontierworks.wraithking;

import com.frontierworks.core.entity.BossMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/** Boss: wither skulls at range, blinks behind its target, raises skeleton guards. */
public class WraithKing extends BossMob {
    private int skullCooldown = 60;
    private int blinkCooldown = 160;
    private int guardCooldown = 200;

    public WraithKing(EntityType<? extends WraithKing> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MOVEMENT_SPEED, 0.29).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, phase()), this);
        return ok;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (t == null) return;
        double d2 = distanceToSqr(t);
        if (--skullCooldown <= 0 && d2 > 16 && hasLineOfSight(t)) {
            skullCooldown = phase() == 1 ? 30 : 60;
            int shots = phase() == 1 ? 3 : 1;
            for (int i = 0; i < shots; i++) {
                double dx = t.getX() - getX() + random.nextGaussian() * 0.6 * i;
                double dy = t.getY(0.5) - getEyeY();
                double dz = t.getZ() - getZ() + random.nextGaussian() * 0.6 * i;
                WitherSkull skull = new WitherSkull(level(), this, dx, dy, dz);
                skull.setPos(getX(), getEyeY(), getZ());
                if (phase() == 1 && i == 0) skull.setDangerous(true);
                level().addFreshEntity(skull);
            }
            playSound(SoundEvents.WITHER_SHOOT, 1.0f, 0.8f);
        }
        if (--blinkCooldown <= 0 && d2 > 100) {
            blinkCooldown = 160;
            double bx = t.getX() - t.getLookAngle().x * 2.5;
            double bz = t.getZ() - t.getLookAngle().z * 2.5;
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 30, 0.5, 1.0, 0.5, 0.05);
            if (randomTeleport(bx, t.getY(), bz, true)) playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }
        if (--guardCooldown <= 0) {
            guardCooldown = 400;
            raiseGuards();
        }
    }

    private void raiseGuards() {
        if (!(level() instanceof ServerLevel sl)) return;
        EntityType<? extends Mob> type = phase() == 1 ? EntityType.WITHER_SKELETON : EntityType.SKELETON;
        for (int i = 0; i < 3; i++) {
            Mob guard = type.create(sl);
            if (guard == null) continue;
            guard.moveTo(getX() + random.nextGaussian() * 3, getY(), getZ() + random.nextGaussian() * 3, random.nextFloat() * 360f, 0);
            guard.finalizeSpawn(sl, sl.getCurrentDifficultyAt(guard.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            guard.setTarget(getTarget());
            sl.addFreshEntity(guard);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, guard.getX(), guard.getY() + 1, guard.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0f, 0.7f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SOUL, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        return effect.getEffect() != MobEffects.WITHER && effect.getEffect() != MobEffects.POISON && super.addEffect(effect, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }
}
