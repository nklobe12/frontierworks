package com.frontierworks.weapons;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/** Sword-like weapon with a fighting style: reach for spears, backstab for daggers, AoE for hammers, shield-break for axes. */
public class WeaponItem extends SwordItem {
    public enum Kind { SPEAR, DAGGER, WARHAMMER, BATTLEAXE }

    private static final UUID REACH = UUID.fromString("0b6f1d32-6c1e-4b7a-9a52-3f0e8d9c2a10");
    public final Kind kind;

    public WeaponItem(Tier tier, Kind kind, int damage, float speed, Properties props) {
        super(tier, damage, speed, props);
        this.kind = kind;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (kind != Kind.SPEAR || slot != EquipmentSlot.MAINHAND) return base;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(base);
        b.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH, "Spear reach", 1.5, AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return kind == Kind.BATTLEAXE || super.canDisableShield(stack, shield, entity, attacker);
    }
}
