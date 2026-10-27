package com.frontierworks.flares;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Thrown flare: lights up where it lands (a temporary light that fades after two minutes). */
public class FlareEntity extends ThrowableItemProjectile {
    public FlareEntity(EntityType<? extends FlareEntity> type, Level level) {
        super(type, level);
    }

    public FlareEntity(EntityType<? extends FlareEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Content.FLARE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0.02, 0);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide) return;
        BlockPos pos = hit instanceof BlockHitResult ? ((BlockHitResult) hit).getBlockPos().relative(((BlockHitResult) hit).getDirection()) : blockPosition();
        BlockState light = Content.FLARE_LIGHT.get().defaultBlockState();
        for (int i = 0; i < 3; i++) {
            if (level().getBlockState(pos).isAir()) {
                level().setBlockAndUpdate(pos, light);
                break;
            }
            pos = pos.above();
        }
        discard();
    }
}
