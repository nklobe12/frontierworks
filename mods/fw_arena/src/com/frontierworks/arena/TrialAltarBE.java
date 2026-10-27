package com.frontierworks.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Runs a trial: spawns waves around the altar, tracks the spawned mobs, rewards when all waves are cleared. */
public class TrialAltarBE extends BlockEntity {
    private static final String[][] NORMAL = {
            {"minecraft:zombie", "minecraft:skeleton", "minecraft:spider"},
            {"minecraft:zombie", "minecraft:skeleton", "minecraft:husk", "minecraft:stray", "fw_mobs:rust_beetle"},
            {"minecraft:skeleton", "minecraft:witch", "minecraft:creeper", "fw_mobs:bog_lurker", "fw_mobs:sand_mummy"},
    };
    private static final String[][] GREATER = {
            {"minecraft:zombie", "minecraft:skeleton", "fw_mobs:rust_beetle", "fw_mobs:cave_crawler"},
            {"minecraft:vindicator", "minecraft:pillager", "fw_mobs:scrap_drone", "fw_mobs:frost_wraith"},
            {"minecraft:wither_skeleton", "minecraft:blaze", "fw_mobs:ember_imp", "minecraft:witch"},
            {"minecraft:vindicator", "minecraft:evoker", "minecraft:wither_skeleton", "fw_mobs:frost_wraith"},
            {"minecraft:ravager", "minecraft:wither_skeleton", "minecraft:evoker", "fw_mobs:scrap_drone"},
    };

    private int wave = -1;          // -1 = idle
    private boolean greater;
    private int delay;
    private int absent;
    private final Set<UUID> alive = new HashSet<>();
    private ServerBossEvent bar;

    public TrialAltarBE(BlockPos pos, BlockState state) {
        super(Content.TRIAL_ALTAR_BE.get(), pos, state);
    }

    public boolean active() {
        return wave >= 0;
    }

    private int waves() {
        return greater ? GREATER.length : NORMAL.length;
    }

    public void start(boolean greater) {
        this.greater = greater;
        this.wave = 0;
        this.delay = 60;
        this.absent = 0;
        alive.clear();
        announce(Component.translatable("msg.fw_arena.started"));
        level.playSound(null, worldPosition, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0f, 1.0f);
        setChanged();
    }

    public void abort() {
        despawn();
        wave = -1;
        if (bar != null) bar.removeAllPlayers();
    }

    private void announce(Component c) {
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(32))) p.displayClientMessage(c, true);
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl) || wave < 0) return;
        if (sl.getGameTime() % 20 != 0) return;
        updateBar(sl);
        List<Player> near = level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(24), p -> !p.isSpectator());
        if (near.isEmpty()) {
            if (++absent >= 30) {
                announce(Component.translatable("msg.fw_arena.failed"));
                abort();
                setChanged();
            }
            return;
        }
        absent = 0;
        sl.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.01);
        alive.removeIf(id -> {
            Entity e = sl.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (!alive.isEmpty()) return;
        if (delay > 0) {
            delay -= 20;
            return;
        }
        if (wave >= waves()) {
            reward(sl);
            wave = -1;
            if (bar != null) bar.removeAllPlayers();
            setChanged();
            return;
        }
        spawnWave(sl, wave);
        announce(Component.translatable("msg.fw_arena.wave", wave + 1, waves()));
        wave++;
        delay = 60;
        setChanged();
    }

    private void updateBar(ServerLevel sl) {
        if (bar == null) bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        bar.setName(Component.translatable("bossbar.fw_arena.trial", Math.max(1, Math.min(wave, waves())), waves()));
        bar.setProgress(Math.max(0f, Math.min(1f, wave / (float) waves())));
        Set<ServerPlayer> near = new HashSet<>(sl.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(32)));
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
        for (ServerPlayer p : near) bar.addPlayer(p);
    }

    private void spawnWave(ServerLevel sl, int w) {
        String[] pool = (greater ? GREATER : NORMAL)[w];
        int count = (greater ? 4 : 3) + w * 2;
        for (int i = 0; i < count; i++) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(pool[sl.random.nextInt(pool.length)]));
            if (type == null) type = EntityType.ZOMBIE;
            Entity e = type.create(sl);
            if (!(e instanceof Mob mob)) continue;
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double r = 7 + sl.random.nextDouble() * 5;
            int x = worldPosition.getX() + (int) Math.round(Math.cos(ang) * r);
            int z = worldPosition.getZ() + (int) Math.round(Math.sin(ang) * r);
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - worldPosition.getY()) > 8) y = worldPosition.getY() + 1;
            mob.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360f, 0);
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 600, 0, false, false));
            if (greater) mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 600, 0, false, false));
            Player target = sl.getNearestPlayer(mob, 32);
            if (target != null) mob.setTarget(target);
            sl.addFreshEntity(mob);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x + 0.5, y + 1, z + 0.5, 12, 0.3, 0.6, 0.3, 0.02);
            alive.add(mob.getUUID());
        }
        sl.playSound(null, worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    private void reward(ServerLevel sl) {
        announce(Component.translatable("msg.fw_arena.cleared"));
        sl.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
        LootTable table = sl.getServer().getLootData().getLootTable(new ResourceLocation("fw_arena", greater ? "rewards/greater_trial" : "rewards/trial"));
        Vec3 c = Vec3.atCenterOf(worldPosition).add(0, 1, 0);
        LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, c).create(LootContextParamSets.CHEST);
        for (ItemStack s : table.getRandomItems(params)) {
            ItemEntity ie = new ItemEntity(sl, c.x, c.y, c.z, s);
            ie.setDeltaMovement(sl.random.nextGaussian() * 0.08, 0.3, sl.random.nextGaussian() * 0.08);
            sl.addFreshEntity(ie);
        }
        ExperienceOrb.award(sl, c, greater ? 400 : 150);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y, c.z, 60, 0.5, 1.0, 0.5, 0.3);
    }

    private void despawn() {
        if (level instanceof ServerLevel sl) {
            for (UUID id : alive) {
                Entity e = sl.getEntity(id);
                if (e != null) e.discard();
            }
        }
        alive.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Wave", wave);
        tag.putBoolean("Greater", greater);
        tag.putInt("Delay", delay);
        ListTag list = new ListTag();
        for (UUID id : alive) list.add(NbtUtils.createUUID(id));
        tag.put("Alive", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        wave = tag.contains("Wave") ? tag.getInt("Wave") : -1;
        greater = tag.getBoolean("Greater");
        delay = tag.getInt("Delay");
        alive.clear();
        for (Tag t : tag.getList("Alive", Tag.TAG_INT_ARRAY)) alive.add(NbtUtils.loadUUID(t));
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (bar != null) bar.removeAllPlayers();
    }

    @SuppressWarnings("unused")
    private static boolean unused(LivingEntity e) {
        return e == null;
    }
}
