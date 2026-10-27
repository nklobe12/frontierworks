package com.lodestar.lapidary;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * What a socketed gem does. Attribute effects are added through ItemAttributeModifierEvent; the others are event hooks
 * in {@link GemEvents}. {@link #perScale} is the amount at scale 1.0 (a Fine step-cut gem is about 1.0).
 */
public enum GemEffect {
    ATTACK_DAMAGE(1.0f, false, () -> Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION),
    ATTACK_SPEED(0.08f, true, () -> Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_BASE),
    MAX_HEALTH(1.0f, false, () -> Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION),
    ARMOR(1.0f, false, () -> Attributes.ARMOR, AttributeModifier.Operation.ADDITION),
    TOUGHNESS(0.6f, false, () -> Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADDITION),
    MOVE_SPEED(0.03f, true, () -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE),
    LUCK(0.5f, false, () -> Attributes.LUCK, AttributeModifier.Operation.ADDITION),
    KNOCKBACK_RESIST(0.05f, true, () -> Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION),
    SWIM_SPEED(0.15f, true, () -> ForgeMod.SWIM_SPEED.get(), AttributeModifier.Operation.MULTIPLY_BASE),
    /** faster digging */
    MINING_SPEED(0.15f, true, null, null),
    /** faster digging under water */
    AQUA_MINING(1.0f, true, null, null),
    /** heals the wielder by a share of the damage dealt */
    LIFESTEAL(0.05f, true, null, null),
    /** extra knockback on hit */
    KNOCKBACK(0.5f, false, null, null),
    /** more experience from mined blocks and slain mobs */
    WISDOM(0.25f, true, null, null),
    /** heals the wearer every five seconds */
    REGEN(0.5f, false, null, null);

    public final float perScale;
    public final boolean percent;
    @Nullable
    private final Supplier<Attribute> attribute;
    @Nullable
    public final AttributeModifier.Operation op;

    GemEffect(float perScale, boolean percent, @Nullable Supplier<Attribute> attribute, @Nullable AttributeModifier.Operation op) {
        this.perScale = perScale;
        this.percent = percent;
        this.attribute = attribute;
        this.op = op;
    }

    @Nullable
    public Attribute attribute() {
        return attribute == null ? null : attribute.get();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String format(float amount) {
        return percent ? String.format(Locale.ROOT, "%.0f%%", amount * 100) : String.format(Locale.ROOT, "%.1f", amount);
    }

    /** "+1.2 attack damage" style line. */
    public Component describe(float amount) {
        return Component.translatable("effect.ls_lapidary." + id(), format(amount));
    }
}
