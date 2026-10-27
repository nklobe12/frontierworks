package com.frontierworks.core.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

import java.util.function.Supplier;

/** Tool tiers and armor materials shared by every Frontierworks gear mod. Repair items are forge tags. */
public final class FWTiers {
    private FWTiers() {}

    private static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(id));
    }

    private static Supplier<Ingredient> tag(String id) {
        return () -> Ingredient.of(itemTag(id));
    }

    private static Tier tier(int level, int uses, float speed, float dmg, int ench, String repair) {
        TagKey<net.minecraft.world.level.block.Block> needs = level >= 4 ? BlockTags.create(new ResourceLocation("forge", "needs_netherite_tool"))
                : level == 3 ? BlockTags.NEEDS_DIAMOND_TOOL : level == 2 ? BlockTags.NEEDS_IRON_TOOL : BlockTags.NEEDS_STONE_TOOL;
        return new ForgeTier(level, uses, speed, dmg, ench, needs, tag(repair));
    }

    public static final Tier BRONZE = tier(2, 350, 6.5f, 2.0f, 15, "forge:ingots/bronze");
    public static final Tier STEEL = tier(2, 750, 7.0f, 2.5f, 12, "forge:ingots/steel");
    public static final Tier INVAR = tier(2, 450, 6.5f, 2.0f, 13, "forge:ingots/invar");
    public static final Tier SILVER = tier(2, 220, 8.0f, 1.5f, 22, "forge:ingots/silver");
    public static final Tier ELECTRUM = tier(2, 180, 11.0f, 1.0f, 26, "forge:ingots/electrum");
    public static final Tier PLATINUM = tier(3, 1100, 8.5f, 3.0f, 18, "forge:ingots/platinum");
    public static final Tier RUBY = tier(3, 1250, 8.0f, 3.0f, 12, "forge:gems/ruby");
    public static final Tier SAPPHIRE = tier(3, 1250, 8.0f, 3.0f, 12, "forge:gems/sapphire");
    public static final Tier PERIDOT = tier(3, 1250, 8.0f, 3.0f, 12, "forge:gems/peridot");
    public static final Tier TOPAZ = tier(3, 1250, 8.0f, 3.0f, 12, "forge:gems/topaz");
    public static final Tier ONYX = tier(4, 2200, 9.0f, 4.0f, 15, "forge:gems/onyx");
    public static final Tier METEORIC = tier(4, 2600, 9.5f, 4.5f, 14, "forge:ingots/meteoric_steel");
    public static final Tier TITANIUM = tier(4, 3000, 10.0f, 5.0f, 16, "forge:ingots/titanium");
    /** Powered tools: unbreakable (energy instead of durability). */
    public static final Tier POWERED = new ForgeTier(3, 0, 9.0f, 3.0f, 0, BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.EMPTY);
    public static final Tier POWERED_ADV = new ForgeTier(4, 0, 14.0f, 4.0f, 0, BlockTags.create(new ResourceLocation("forge", "needs_netherite_tool")), () -> Ingredient.EMPTY);

    public static Tier byName(String name) {
        switch (name) {
            case "bronze": return BRONZE;
            case "steel": return STEEL;
            case "invar": return INVAR;
            case "silver": return SILVER;
            case "electrum": return ELECTRUM;
            case "platinum": return PLATINUM;
            case "ruby": return RUBY;
            case "sapphire": return SAPPHIRE;
            case "peridot": return PERIDOT;
            case "topaz": return TOPAZ;
            case "onyx": return ONYX;
            case "meteoric_steel": return METEORIC;
            case "titanium": return TITANIUM;
            default: throw new IllegalArgumentException(name);
        }
    }

    /** Armor material with per-piece defense (helmet, chest, legs, boots). */
    public static class Armor implements ArmorMaterial {
        private static final int[] BASE = {11, 16, 15, 13};
        private final String name;
        private final int multiplier;
        private final int[] defense;
        private final int enchant;
        private final float toughness;
        private final float knockback;
        private final Supplier<Ingredient> repair;
        private final SoundEvent sound;

        public Armor(String name, int multiplier, int[] defense, int enchant, float toughness, float knockback, String repairTag, SoundEvent sound) {
            this.name = name;
            this.multiplier = multiplier;
            this.defense = defense;
            this.enchant = enchant;
            this.toughness = toughness;
            this.knockback = knockback;
            this.repair = repairTag == null ? () -> Ingredient.EMPTY : tag(repairTag);
            this.sound = sound;
        }

        private static int idx(ArmorItem.Type type) {
            switch (type) {
                case HELMET: return 0;
                case CHESTPLATE: return 1;
                case LEGGINGS: return 2;
                default: return 3;
            }
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return BASE[idx(type)] * multiplier;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return defense[idx(type)];
        }

        @Override
        public int getEnchantmentValue() {
            return enchant;
        }

        @Override
        public SoundEvent getEquipSound() {
            return sound;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return repair.get();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public float getToughness() {
            return toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return knockback;
        }
    }

    public static Armor armor(String fullName, int mult, int helmet, int chest, int legs, int boots, int ench, float tough, float kb, String repair) {
        return new Armor(fullName, mult, new int[]{helmet, chest, legs, boots}, ench, tough, kb, repair, SoundEvents.ARMOR_EQUIP_IRON);
    }
}
