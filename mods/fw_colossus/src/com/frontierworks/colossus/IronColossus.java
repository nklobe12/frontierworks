package com.frontierworks.colossus;

import com.frontierworks.core.entity.BossMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Boss: ground slams, launches targets, calls scrap drones when below half health. */
public class IronColossus extends BossMob {
    private int slamCooldown = 100;
    private int summonCooldown = 160;

    public IronColossus(EntityType<? extends IronColossus> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 320.0).add(Attributes.ARMOR, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) target.setDeltaMovement(target.getDeltaMovement().add(0, 0.7, 0));
        return ok;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (t == null) return;
        if (--slamCooldown <= 0 && distanceToSqr(t) < 64 && onGround()) slam();
        if (phase() == 1 && --summonCooldown <= 0) summonDrones();
    }

    private void slam() {
        slamCooldown = phase() == 1 ? 80 : 140;
        playSound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f);
        if (level() instanceof ServerLevel sl) {
            BlockState ground = level().getBlockState(blockPosition().below());
            if (ground.isAir()) ground = Blocks.STONE.defaultBlockState();
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.2, getZ(), 120, 3.0, 0.2, 3.0, 0.2);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 4, 1.5, 0.3, 1.5, 0.0);
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7.0, 2.0, 7.0), e -> e != this && !(e instanceof IronColossus))) {
            double d = Math.max(1.0, Math.sqrt(distanceToSqr(e)));
            e.hurt(damageSources().mobAttack(this), (float) (16.0 - d));
            Vec3 push = e.position().subtract(position()).normalize().scale(1.4);
            e.push(push.x, 0.8, push.z);
            e.hurtMarked = true;
        }
    }

    private void summonDrones() {
        summonCooldown = 400;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("fw_mobs", "scrap_drone"));
        if (type == null) return;
        for (int i = 0; i < 2; i++) {
            Entity drone = type.create(level());
            if (drone == null) continue;
            drone.moveTo(getX() + random.nextGaussian() * 2, getY() + 4, getZ() + random.nextGaussian() * 2, 0, 0);
            level().addFreshEntity(drone);
        }
        level().playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0f, 0.6f);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.5f, 0.6f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }
}
