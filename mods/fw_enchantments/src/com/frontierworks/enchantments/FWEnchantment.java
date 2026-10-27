package com.frontierworks.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class FWEnchantment extends Enchantment {
    private final int max;
    private final int base;
    private final int perLevel;
    private final boolean treasure;

    public FWEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots, int max, int base, int perLevel, boolean treasure) {
        super(rarity, category, slots);
        this.max = max;
        this.base = base;
        this.perLevel = perLevel;
        this.treasure = treasure;
    }

    @Override
    public int getMaxLevel() {
        return max;
    }

    @Override
    public int getMinCost(int level) {
        return base + (level - 1) * Math.max(perLevel, 1);
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    @Override
    public boolean isTreasureOnly() {
        return treasure;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        if (this == Content.SMELTING.get() && other == Enchantments.SILK_TOUCH) return false;
        if (this == Content.FROSTBITE.get() && other == Enchantments.FIRE_ASPECT) return false;
        return super.checkCompatibility(other);
    }
}
