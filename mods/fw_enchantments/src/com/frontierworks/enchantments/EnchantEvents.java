package com.frontierworks.enchantments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EnchantEvents {
    private static final UUID REACH_ID = UUID.fromString("7c2b9f40-3a1d-4e6b-8f2a-5d9c1e0b7a11");
    private static final Map<UUID, List<ItemStack>> SOULBOUND = new HashMap<>();

    private EnchantEvents() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(EnchantEvents::onHurt);
        MinecraftForge.EVENT_BUS.addListener(EnchantEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(EnchantEvents::onXp);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, EnchantEvents::onDrops);
        MinecraftForge.EVENT_BUS.addListener(EnchantEvents::onClone);
        MinecraftForge.EVENT_BUS.addListener(EnchantEvents::onAttributes);
    }

    private static int level(Enchantment e, ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getEnchantmentLevel(e);
    }

    private static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getSource().getDirectEntity() != attacker) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        ItemStack weapon = attacker.getMainHandItem();
        int ls = level(Content.LIFESTEAL.get(), weapon);
        if (ls > 0) attacker.heal(event.getAmount() * 0.05f * ls);
        int fb = level(Content.FROSTBITE.get(), weapon);
        if (fb > 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 * fb, fb - 1));
        int vn = level(Content.VENOM.get(), weapon);
        if (vn > 0) target.addEffect(new MobEffectInstance(MobEffects.POISON, 50 * vn, vn - 1));
        if (level(Content.THUNDERSTRIKE.get(), weapon) > 0 && attacker.getRandom().nextFloat() < 0.15f) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(target.level());
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                if (attacker instanceof ServerPlayer sp) bolt.setCause(sp);
                target.level().addFreshEntity(bolt);
            }
        }
    }

    private static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || e.tickCount % 20 != 0 || !(e instanceof Player)) return;
        ItemStack boots = e.getItemBySlot(EquipmentSlot.FEET);
        int sw = level(Content.SWIFTSTEP.get(), boots);
        if (sw > 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, sw - 1, true, false, false));
        int lp = level(Content.LEAPING.get(), boots);
        if (lp > 0) e.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, lp - 1, true, false, false));
    }

    private static void onXp(LivingExperienceDropEvent event) {
        Player p = event.getAttackingPlayer();
        if (p == null) return;
        int w = level(Content.WISDOM.get(), p.getMainHandItem());
        if (w > 0) event.setDroppedExperience((int) (event.getDroppedExperience() * (1 + 0.5 * w)));
    }

    private static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        // Beheading
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            int bh = level(Content.BEHEADING.get(), killer.getMainHandItem());
            if (bh > 0 && killer.getRandom().nextFloat() < 0.05f * bh) {
                ItemStack head = headFor(dead.getType());
                if (!head.isEmpty()) event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), head));
            }
        }
        // Soulbound: keep matching items for the respawned player
        if (dead instanceof ServerPlayer player) {
            List<ItemStack> kept = new ArrayList<>();
            Iterator<ItemEntity> it = event.getDrops().iterator();
            while (it.hasNext()) {
                ItemEntity ie = it.next();
                if (level(Content.SOULBOUND.get(), ie.getItem()) > 0) {
                    kept.add(ie.getItem().copy());
                    it.remove();
                }
            }
            if (!kept.isEmpty()) SOULBOUND.put(player.getUUID(), kept);
        }
    }

    private static ItemStack headFor(EntityType<?> type) {
        if (type == EntityType.ZOMBIE) return new ItemStack(Items.ZOMBIE_HEAD);
        if (type == EntityType.SKELETON) return new ItemStack(Items.SKELETON_SKULL);
        if (type == EntityType.CREEPER) return new ItemStack(Items.CREEPER_HEAD);
        if (type == EntityType.WITHER_SKELETON) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (type == EntityType.PIGLIN) return new ItemStack(Items.PIGLIN_HEAD);
        return ItemStack.EMPTY;
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        List<ItemStack> kept = SOULBOUND.remove(event.getOriginal().getUUID());
        if (kept == null) return;
        for (ItemStack s : kept) {
            if (!event.getEntity().getInventory().add(s)) event.getEntity().drop(s, false);
        }
    }

    private static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) return;
        int r = level(Content.LONG_REACH.get(), event.getItemStack());
        if (r > 0) {
            event.addModifier(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(REACH_ID, "Long Reach", r, AttributeModifier.Operation.ADDITION));
        }
    }
}
