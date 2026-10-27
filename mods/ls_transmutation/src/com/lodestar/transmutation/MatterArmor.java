package com.lodestar.transmutation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Umbral and radiant matter armor: unbreakable; a full set grants protections (no fall damage, fire immunity...). */
public class MatterArmor extends ArmorItem {
    public record Material(String name, int[] defense, float toughness, float knockback, Supplier<Ingredient> repair) implements ArmorMaterial {
        @Override
        public int getDurabilityForType(Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(Type type) {
            return defense[type.ordinal()];
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
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

    // defense order follows ArmorItem.Type: HELMET, CHESTPLATE, LEGGINGS, BOOTS
    public static final Material UMBRAL = new Material("ls_transmutation:umbral", new int[]{3, 8, 6, 3}, 3f, 0.1f, () -> Ingredient.of(Content.UMBRAL_MATTER.get()));
    public static final Material RADIANT = new Material("ls_transmutation:radiant", new int[]{4, 9, 7, 4}, 4f, 0.2f, () -> Ingredient.of(Content.RADIANT_MATTER.get()));

    public MatterArmor(Material mat, Type type, Properties props) {
        super(mat, type, props.fireResistant());
    }

    /** Number of pieces of a material the player wears. */
    public static int pieces(Player p, Material mat) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (st.getItem() instanceof MatterArmor a && a.getMaterial() == mat) n++;
        }
        return n;
    }

    /** Full-set effects, called every second from a player tick. */
    public static void tick(Player p) {
        boolean umbral = pieces(p, UMBRAL) + pieces(p, RADIANT) >= 4;
        boolean radiant = pieces(p, RADIANT) >= 4;
        if (umbral) p.fallDistance = 0;
        if (radiant) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getMaterial() == RADIANT ? "tooltip.ls_transmutation.radiant_set" : "tooltip.ls_transmutation.umbral_set")
                .withStyle(ChatFormatting.AQUA));
    }
}
