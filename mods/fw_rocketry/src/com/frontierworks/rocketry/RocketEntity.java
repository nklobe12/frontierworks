package com.frontierworks.rocketry;

import com.frontierworks.core.util.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** A rideable rocket: fuel it, board it, press jump. Flies to the Moon/Mars (or home) and lands at the destination. */
public class RocketEntity extends Entity {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, COUNTDOWN = 1, FLYING = 2, LANDING = 3;
    private int timer;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FUEL, 0);
        entityData.define(TIER, 1);
        entityData.define(STATE, IDLE);
    }

    public int fuel() { return entityData.get(FUEL); }
    public int tier() { return entityData.get(TIER); }
    public int state() { return entityData.get(STATE); }
    public void setFuel(int f) { entityData.set(FUEL, f); }
    public void setTier(int t) { entityData.set(TIER, t); }
    public void setState(int s) { entityData.set(STATE, s); }

    public static int fuelNeeded(int tier) {
        return tier >= 2 ? 8 : 4;
    }

    public String destination() {
        String here = level().dimension().location().toString();
        if (!here.equals("minecraft:overworld")) return "minecraft:overworld";
        return tier() >= 2 ? "fw_mars:mars" : "fw_moon:moon";
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 1.2;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Content.ROCKET_FUEL.get())) {
            if (!level().isClientSide && fuel() < fuelNeeded(tier())) {
                setFuel(fuel() + 1);
                if (!player.getAbilities().instabuild) held.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 0.8f);
                player.displayClientMessage(Component.translatable("msg.fw_rocketry.fuel", fuel(), fuelNeeded(tier())), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isShiftKeyDown()) {
            if (!level().isClientSide && state() == IDLE && getPassengers().isEmpty()) {
                ItemStack item = new ItemStack(tier() >= 2 ? Content.ROCKET_T2.get() : Content.ROCKET_T1.get());
                item.getOrCreateTag().putInt("Fuel", fuel());
                if (!player.getInventory().add(item)) player.drop(item, false);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide && player.startRiding(this)) {
            player.displayClientMessage(Component.translatable("msg.fw_rocketry.board"), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void tryLaunch(ServerPlayer player) {
        if (state() != IDLE) return;
        if (fuel() < fuelNeeded(tier())) {
            player.displayClientMessage(Component.translatable("msg.fw_rocketry.need_fuel", fuel(), fuelNeeded(tier())), true);
            return;
        }
        if (TeleportUtil.level(player.server, destination()) == null) {
            player.displayClientMessage(Component.translatable("msg.fw_rocketry.no_dest"), true);
            return;
        }
        setFuel(0);
        setState(COUNTDOWN);
        timer = 0;
        player.displayClientMessage(Component.translatable("msg.fw_rocketry.launch"), true);
    }

    @Override
    public void tick() {
        super.tick();
        int st = state();
        if (level().isClientSide) {
            if (st == COUNTDOWN || st == FLYING || st == LANDING) {
                for (int i = 0; i < (st == FLYING ? 6 : 2); i++) {
                    level().addParticle(st == COUNTDOWN ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.FLAME,
                            getX() + (random.nextDouble() - 0.5) * 0.8, getY() - 0.2, getZ() + (random.nextDouble() - 0.5) * 0.8,
                            (random.nextDouble() - 0.5) * 0.1, st == LANDING ? -0.05 : -0.3, (random.nextDouble() - 0.5) * 0.1);
                }
            }
            return;
        }
        Vec3 v = getDeltaMovement();
        switch (st) {
            case COUNTDOWN -> {
                setDeltaMovement(Vec3.ZERO);
                if (timer++ % 20 == 0) level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 1.0f, 0.8f + timer / 60f);
                if (timer >= 60) {
                    setState(FLYING);
                    timer = 0;
                    level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 3.0f, 0.5f);
                }
            }
            case FLYING -> {
                timer++;
                setDeltaMovement(0, Math.min(0.05 + timer * 0.015, 2.5), 0);
                if (timer % 10 == 0) level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0f, 0.4f);
                if (getY() > level().getMaxBuildHeight() + 40 || timer > 400) arrive();
            }
            case LANDING -> {
                setDeltaMovement(0, -0.35, 0);
                if (onGround()) setState(IDLE);
            }
            default -> setDeltaMovement(0, onGround() ? 0 : Math.max(v.y - 0.04, -0.6), 0);
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    private void arrive() {
        if (!(level() instanceof ServerLevel here)) return;
        ServerLevel dest = TeleportUtil.level(here.getServer(), destination());
        if (dest == null) {
            setState(LANDING);
            return;
        }
        List<Entity> riders = new ArrayList<>(getPassengers());
        ejectPassengers();
        BlockPos ground = TeleportUtil.findSafe(dest, (int) Math.floor(getX()), (int) Math.floor(getZ()), Blocks.STONE.defaultBlockState());
        double y = Math.min(ground.getY() + 50, dest.getMaxBuildHeight() - 8);
        RocketEntity copy = Content.ROCKET.get().create(dest);
        if (copy == null) return;
        copy.setTier(tier());
        copy.setState(LANDING);
        copy.moveTo(ground.getX() + 0.5, y, ground.getZ() + 0.5, getYRot(), 0);
        dest.addFreshEntity(copy);
        for (Entity e : riders) {
            if (e instanceof ServerPlayer p) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
                TeleportUtil.teleport(p, dest, copy.getX(), y + 1.2, copy.getZ(), p.getYRot(), p.getXRot());
                p.startRiding(copy, true);
            }
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFuel(tag.getInt("Fuel"));
        setTier(Math.max(1, tag.getInt("Tier")));
        int st = tag.getInt("State");
        setState(st == FLYING || st == COUNTDOWN ? IDLE : st);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuel", fuel());
        tag.putInt("Tier", tier());
        tag.putInt("State", state());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
