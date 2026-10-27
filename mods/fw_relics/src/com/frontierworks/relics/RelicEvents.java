package com.frontierworks.relics;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Relics work from anywhere in the inventory; checked once a second (attributes) or when relevant events fire. */
public final class RelicEvents {
    private static final UUID VIGOR = UUID.fromString("e6a1c7d0-1b2a-4c55-9e10-7a8b9c0d1e21");
    private static final UUID LUCK = UUID.fromString("e6a1c7d0-1b2a-4c55-9e10-7a8b9c0d1e22");
    private static final UUID ARMOR = UUID.fromString("e6a1c7d0-1b2a-4c55-9e10-7a8b9c0d1e23");
    private static final UUID TOUGH = UUID.fromString("e6a1c7d0-1b2a-4c55-9e10-7a8b9c0d1e24");

    private RelicEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(RelicEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(RelicEvents::onFall);
        MinecraftForge.EVENT_BUS.addListener(RelicEvents::onAttack);
        MinecraftForge.EVENT_BUS.addListener(RelicEvents::onHurt);
        MinecraftForge.EVENT_BUS.addListener(RelicEvents::onDeath);
    }

    public static Set<RelicItem.Kind> carried(Player p) {
        Set<RelicItem.Kind> set = EnumSet.noneOf(RelicItem.Kind.class);
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof RelicItem r) set.add(r.kind);
        for (ItemStack s : p.getInventory().offhand) if (s.getItem() instanceof RelicItem r) set.add(r.kind);
        return set;
    }

    private static ItemStack find(Player p, RelicItem.Kind kind) {
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof RelicItem r && r.kind == kind) return s;
        for (ItemStack s : p.getInventory().offhand) if (s.getItem() instanceof RelicItem r && r.kind == kind) return s;
        return ItemStack.EMPTY;
    }

    private static void modifier(Player p, Attribute attr, UUID id, String name, double amount, boolean on) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (on && !has) inst.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        if (!on && has) inst.removeModifier(id);
    }

    private static void effect(Player p, MobEffect e, int amp) {
        p.addEffect(new MobEffectInstance(e, e == MobEffects.NIGHT_VISION ? 300 : 60, amp, true, false, true));
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) return;
        Player p = event.player;
        Set<RelicItem.Kind> k = carried(p);
        modifier(p, Attributes.MAX_HEALTH, VIGOR, "Amulet of Vigor", 4.0, k.contains(RelicItem.Kind.VIGOR));
        if (!k.contains(RelicItem.Kind.VIGOR) && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        modifier(p, Attributes.LUCK, LUCK, "Lucky Coin", 1.0, k.contains(RelicItem.Kind.LUCK));
        modifier(p, Attributes.ARMOR, ARMOR, "Warding Amulet", 3.0, k.contains(RelicItem.Kind.WARDING));
        modifier(p, Attributes.ARMOR_TOUGHNESS, TOUGH, "Warding Amulet", 2.0, k.contains(RelicItem.Kind.WARDING));
        if (k.contains(RelicItem.Kind.HASTE)) effect(p, MobEffects.DIG_SPEED, 0);
        if (k.contains(RelicItem.Kind.SWIFT)) effect(p, MobEffects.MOVEMENT_SPEED, 0);
        if (k.contains(RelicItem.Kind.EMBER)) effect(p, MobEffects.FIRE_RESISTANCE, 0);
        if (k.contains(RelicItem.Kind.TIDAL)) {
            effect(p, MobEffects.WATER_BREATHING, 0);
            if (p.isInWater()) effect(p, MobEffects.DOLPHINS_GRACE, 0);
        }
        if (k.contains(RelicItem.Kind.OWL)) {
            MobEffectInstance nv = p.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 220) effect(p, MobEffects.NIGHT_VISION, 0);
        }
        if (k.contains(RelicItem.Kind.REGEN) && p.tickCount % 100 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1.0f);
    }

    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && carried(p).contains(RelicItem.Kind.FEATHER)) event.setDamageMultiplier(0.0f);
    }

    private static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player p && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && carried(p).contains(RelicItem.Kind.EMBER)) {
            event.setCanceled(true);
        }
    }

    private static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player p && event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && event.getSource().getEntity() == attacker && attacker != p && carried(p).contains(RelicItem.Kind.THORNS)) {
            attacker.hurt(p.damageSources().thorns(p), event.getAmount() * 0.25f);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack feather = find(p, RelicItem.Kind.PHOENIX);
        if (feather.isEmpty() || p.getCooldowns().isOnCooldown(feather.getItem())) return;
        event.setCanceled(true);
        p.setHealth(p.getMaxHealth() * 0.5f);
        p.removeAllEffects();
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        p.getCooldowns().addCooldown(feather.getItem(), 12000);
        p.level().broadcastEntityEvent(p, (byte) 35);
        p.displayClientMessage(Component.translatable("msg.fw_relics.phoenix"), true);
    }

    @SuppressWarnings("unused")
    private static boolean unused() {
        return Items.AIR == null;
    }
}
