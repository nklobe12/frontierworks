package com.frontierworks.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.HashMap;
import java.util.Map;

/** Full-set bonuses: wearing all four pieces of a material grants a permanent effect. */
public final class SetBonuses {
    private static final Map<String, MobEffect> BONUS = new HashMap<>();
    private static final Map<String, Integer> LEVEL = new HashMap<>();

    static {
        BONUS.put("fw_armor:ruby", MobEffects.DAMAGE_BOOST);
        BONUS.put("fw_armor:sapphire", MobEffects.WATER_BREATHING);
        BONUS.put("fw_armor:peridot", MobEffects.LUCK);
        BONUS.put("fw_armor:topaz", MobEffects.DIG_SPEED);
        BONUS.put("fw_armor:onyx", MobEffects.FIRE_RESISTANCE);
        BONUS.put("fw_armor:meteoric_steel", MobEffects.DAMAGE_RESISTANCE);
        BONUS.put("fw_armor:titanium", MobEffects.MOVEMENT_SPEED);
        BONUS.put("fw_armor:silver", MobEffects.NIGHT_VISION);
        BONUS.put("fw_armor:platinum", MobEffects.HEALTH_BOOST);
        LEVEL.put("fw_armor:platinum", 1);
    }

    private SetBonuses() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(SetBonuses::onTick);
        MinecraftForge.EVENT_BUS.addListener(SetBonuses::onTooltip);
    }

    private static String fullSet(Player p) {
        String name = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(slot);
            if (!(st.getItem() instanceof ArmorItem)) return null;
            String n = ((ArmorItem) st.getItem()).getMaterial().getName();
            if (name == null) name = n;
            else if (!name.equals(n)) return null;
        }
        return name;
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 40 != 0) return;
        String set = fullSet(event.player);
        if (set == null) return;
        MobEffect effect = BONUS.get(set);
        if (effect == null) return;
        int dur = effect == MobEffects.NIGHT_VISION ? 300 : 80;
        event.player.addEffect(new MobEffectInstance(effect, dur, LEVEL.getOrDefault(set, 0), true, false, true));
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ArmorItem)) return;
        ArmorMaterial mat = ((ArmorItem) event.getItemStack().getItem()).getMaterial();
        MobEffect effect = BONUS.get(mat.getName());
        if (effect != null) {
            event.getToolTip().add(Component.translatable("tooltip.fw_armor.set_bonus", effect.getDisplayName()).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
