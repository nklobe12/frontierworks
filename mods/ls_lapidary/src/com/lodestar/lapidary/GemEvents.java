package com.lodestar.lapidary;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/** Applies socketed gem bonuses: attributes through ItemAttributeModifierEvent, everything else through events. */
public final class GemEvents {
    private GemEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(GemEvents::attributes);
        MinecraftForge.EVENT_BUS.addListener(GemEvents::breakSpeed);
        MinecraftForge.EVENT_BUS.addListener(GemEvents::hurt);
        MinecraftForge.EVENT_BUS.addListener(GemEvents::mobXp);
        MinecraftForge.EVENT_BUS.addListener(GemEvents::blockXp);
        MinecraftForge.EVENT_BUS.addListener(GemEvents::playerTick);
    }

    public static UUID uuid(EquipmentSlot slot, GemEffect effect) {
        return UUID.nameUUIDFromBytes(("ls_lapidary:" + slot.getName() + ":" + effect.id()).getBytes(StandardCharsets.UTF_8));
    }

    /** Gems give their attribute bonuses only in the slot the item is made for (main hand for tools, its slot for armor). */
    private static void attributes(ItemAttributeModifierEvent e) {
        ItemStack st = e.getItemStack();
        if (!GemData.hasSockets(st)) return;
        EquipmentSlot natural = LivingEntity.getEquipmentSlotForItem(st);
        if (e.getSlotType() != natural) return;
        boolean armor = natural.getType() == EquipmentSlot.Type.ARMOR;
        for (Map.Entry<GemEffect, Float> en : GemData.totals(st, armor).entrySet()) {
            GemEffect ef = en.getKey();
            Attribute a = ef.attribute();
            if (a == null || ef.op == null) continue;
            e.addModifier(a, new AttributeModifier(uuid(natural, ef), "Socketed gems", en.getValue(), ef.op));
        }
    }

    private static void breakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        ItemStack tool = p.getMainHandItem();
        if (!GemData.hasSockets(tool)) return;
        float speed = e.getNewSpeed() * (1f + GemData.amount(tool, GemEffect.MINING_SPEED));
        if (p.isEyeInFluid(FluidTags.WATER)) speed *= 1f + GemData.amount(tool, GemEffect.AQUA_MINING);
        e.setNewSpeed(speed);
    }

    private static void hurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof LivingEntity attacker) || e.getSource().getDirectEntity() != attacker) return;
        ItemStack weapon = attacker.getMainHandItem();
        if (!GemData.hasSockets(weapon)) return;
        float steal = GemData.amount(weapon, GemEffect.LIFESTEAL);
        if (steal > 0) attacker.heal(e.getAmount() * steal);
        float kb = GemData.amount(weapon, GemEffect.KNOCKBACK);
        if (kb > 0) {
            LivingEntity target = e.getEntity();
            double dx = attacker.getX() - target.getX(), dz = attacker.getZ() - target.getZ();
            target.knockback(kb * 0.5, dx, dz);
        }
    }

    private static void mobXp(LivingExperienceDropEvent e) {
        Player p = e.getAttackingPlayer();
        if (p == null) return;
        float w = GemData.amount(p.getMainHandItem(), GemEffect.WISDOM);
        if (w > 0) e.setDroppedExperience(Math.round(e.getDroppedExperience() * (1f + w)));
    }

    private static void blockXp(BlockEvent.BreakEvent e) {
        if (e.getPlayer() == null || e.getExpToDrop() <= 0) return;
        float w = GemData.amount(e.getPlayer().getMainHandItem(), GemEffect.WISDOM);
        if (w > 0) e.setExpToDrop(Math.round(e.getExpToDrop() * (1f + w)));
    }

    /** Peridot armor slowly heals its wearer. */
    private static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide || e.player.tickCount % 100 != 0) return;
        Player p = e.player;
        if (p.getHealth() >= p.getMaxHealth() || p.isDeadOrDying()) return;
        float regen = GemData.worn(p, GemEffect.REGEN);
        if (regen > 0) p.heal(regen);
    }
}
