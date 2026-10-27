package com.lodestar.toolsmith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * Behaviour of material traits and modifiers. Called from {@link ModularToolItem} and from Forge events.
 * Trait levels count how many parts of the tool carry the trait.
 */
public final class Traits {
    private Traits() {}

    /** Mining speed multiplier from traits for the current situation. */
    public static float speedMultiplier(ItemStack tool, BlockState state, Entity user) {
        Map<String, Integer> t = ToolData.traits(tool);
        float m = 1f;
        float worn = tool.getMaxDamage() > 0 ? (float) tool.getDamageValue() / tool.getMaxDamage() : 0f;
        m *= 1 + 0.6f * worn * t.getOrDefault("stonebound", 0);
        m += 0.15f * t.getOrDefault("fractured", 0);
        if (user != null && (user.isInWaterOrRain())) m *= 1 + 0.5f * t.getOrDefault("aquadynamic", 0);
        if (worn < 0.1f) m *= 1 + 0.2f * t.getOrDefault("crystalline", 0);
        if (user != null && user.level().dimension() == Level.NETHER) m *= 1 + 0.15f * t.getOrDefault("fireproof", 0);
        int mom = tool.getOrCreateTagElement(ToolData.TAG).getInt("Momentum");
        if (t.containsKey("momentum")) m *= 1 + Math.min(1f, mom / 40f) * t.get("momentum");
        return m;
    }

    /** Extra attack damage from traits and modifiers against a target. */
    public static float bonusDamage(ItemStack tool, LivingEntity target, float base) {
        Map<String, Integer> t = ToolData.traits(tool);
        float bonus = 0;
        float worn = tool.getMaxDamage() > 0 ? (float) tool.getDamageValue() / tool.getMaxDamage() : 0f;
        bonus += base * 0.5f * worn * t.getOrDefault("jagged", 0);
        bonus -= base * 0.3f * worn * t.getOrDefault("stonebound", 0);
        if (target.getMobType() == MobType.UNDEAD) {
            bonus += base * 0.5f * t.getOrDefault("holy", 0);
            bonus += 2.5f * ToolData.modifier(tool, "smite");
        }
        if (target.getArmorValue() == 0) bonus += 1.0f * t.getOrDefault("crude", 0);
        return bonus;
    }

    /** Effects applied when the tool hits a living target. */
    public static void onHit(ItemStack tool, LivingEntity target, LivingEntity attacker) {
        Map<String, Integer> t = ToolData.traits(tool);
        int fire = t.getOrDefault("hellfire", 0) + ToolData.modifier(tool, "fiery");
        if (fire > 0) target.setSecondsOnFire(3 * fire);
        if (t.getOrDefault("prickly", 0) > 0) {
            target.hurt(attacker.damageSources().cactus(), t.get("prickly"));
            if (attacker.getRandom().nextFloat() < 0.3f) attacker.hurt(attacker.damageSources().cactus(), 1);
        }
        int kb = t.getOrDefault("bouncy", 0) + ToolData.modifier(tool, "knockback") + t.getOrDefault("heavy", 0);
        if (kb > 0) target.knockback(0.4f * kb, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        int shock = t.getOrDefault("shocking", 0);
        if (shock > 0 && attacker.getRandom().nextFloat() < 0.25f * shock && attacker.level() instanceof ServerLevel sl) {
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4), e -> e != attacker && e != target && e.isAlive())) {
                e.hurt(attacker.damageSources().lightningBolt(), 2.0f * shock);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 6, 0.2, 0.3, 0.2, 0.05);
                break;
            }
        }
    }

    /** Chance-based durability savings; returns how much damage to actually apply. */
    public static int reduceDamage(ItemStack tool, int amount, net.minecraft.util.RandomSource random) {
        Map<String, Integer> t = ToolData.traits(tool);
        float save = 0.2f * t.getOrDefault("hardened", 0) + 0.15f * t.getOrDefault("reinforced", 0) + 0.1f * t.getOrDefault("dense", 0)
                + 0.15f * ToolData.modifier(tool, "reinforced");
        save = Math.min(0.85f, save);
        int out = 0;
        for (int i = 0; i < amount; i++) if (random.nextFloat() >= save) out++;
        return out;
    }

    /** After a block is broken by the tool. */
    public static void onBlockBroken(ItemStack tool, Level level, BlockPos pos, BlockState state, LivingEntity user) {
        Map<String, Integer> t = ToolData.traits(tool);
        if (t.containsKey("momentum")) {
            var tag = tool.getOrCreateTagElement(ToolData.TAG);
            tag.putInt("Momentum", Math.min(40, tag.getInt("Momentum") + 2));
            tag.putLong("MomentumAt", level.getGameTime());
        }
        if (t.getOrDefault("magnetic", 0) > 0 && level instanceof ServerLevel sl) {
            int r = 2 + 2 * t.get("magnetic");
            for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(r))) {
                it.setPos(user.getX(), user.getY() + 0.5, user.getZ());
                it.setPickUpDelay(0);
            }
        }
        int xpBonus = t.getOrDefault("prosperous", 0) + t.getOrDefault("resonant", 0);
        if (xpBonus > 0 && state.is(net.minecraftforge.common.Tags.Blocks.ORES) && level instanceof ServerLevel sl && level.random.nextFloat() < 0.35f * xpBonus) {
            ExperienceOrb.award(sl, pos.getCenter(), 1 + level.random.nextInt(2 * xpBonus));
        }
    }

    /** Every tick while in an inventory (on the server). */
    public static void inventoryTick(ItemStack tool, Level level, Entity holder) {
        Map<String, Integer> t = ToolData.traits(tool);
        long time = level.getGameTime();
        if (t.getOrDefault("ecological", 0) > 0 && time % (160 / t.get("ecological")) == 0 && level.isDay()
                && level.canSeeSky(holder.blockPosition()) && tool.getDamageValue() > 0) {
            tool.setDamageValue(tool.getDamageValue() - 1);
        }
        if (t.getOrDefault("galvanized", 0) > 0 && time % (1200 / t.get("galvanized")) == 0 && tool.getDamageValue() > 0) {
            tool.setDamageValue(tool.getDamageValue() - 1);
        }
        if (t.containsKey("momentum")) {
            var tag = tool.getOrCreateTagElement(ToolData.TAG);
            if (tag.getInt("Momentum") > 0 && time - tag.getLong("MomentumAt") > 40) tag.putInt("Momentum", 0);
        }
    }

    /** Fortune/looting bonus from traits and the luck modifier. */
    public static int fortune(ItemStack tool) {
        Map<String, Integer> t = ToolData.traits(tool);
        return t.getOrDefault("lucky", 0) + t.getOrDefault("refined", 0) + ToolData.modifier(tool, "luck");
    }

    /** Tool experience for breaking a block. */
    public static int blockXp(ItemStack tool, BlockState state) {
        int xp = state.is(net.minecraftforge.common.Tags.Blocks.ORES) ? 3 : 1;
        return Math.round(xp * (1 + 0.5f * ToolData.trait(tool, "resonant")));
    }

    public static boolean isLog(BlockState s) {
        return s.is(BlockTags.LOGS);
    }
}
