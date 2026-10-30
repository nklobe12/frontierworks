package com.lodestar.jetpacks;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lodestar.core.energy.IEnergyItem;
import com.lodestar.core.energy.ItemEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A jetpack, worn in the chest slot. Fuel tiers hold liquid fuel (fill them on a tank, from a fuel canister or in a
 * bottler); electric tiers hold FE. Upgrades and the engine/hover switches live in the stack's NBT.
 */
public class JetpackItem extends ArmorItem implements IEnergyItem {
    public static final String MODS = "JetMods", ENGINE_OFF = "EngineOff", HOVER = "Hover", BURN = "Burn";
    private static final UUID ARMOR_ID = UUID.fromString("3c6f1a1e-6c9b-4f0a-9e52-7a1c0b5d2e41");
    public final JetpackTier tier;

    public JetpackItem(JetpackTier tier, Properties props) {
        super(material(tier), Type.CHESTPLATE, props.stacksTo(1));
        this.tier = tier;
    }

    private static ArmorMaterial material(JetpackTier t) {
        return new ArmorMaterial() {
            public int getDurabilityForType(@Nonnull Type type) {
                return 0;
            }

            public int getDefenseForType(@Nonnull Type type) {
                return 0;
            }

            public int getEnchantmentValue() {
                return 8;
            }

            @Nonnull
            public SoundEvent getEquipSound() {
                return SoundEvents.ARMOR_EQUIP_IRON;
            }

            @Nonnull
            public Ingredient getRepairIngredient() {
                return Ingredient.EMPTY;
            }

            @Nonnull
            public String getName() {
                return "ls_jetpacks:jetpack";
            }

            public float getToughness() {
                return 0;
            }

            public float getKnockbackResistance() {
                return 0;
            }
        };
    }

    // ------------------------------------------------------------------ upgrades and switches

    public enum Mod {
        THRUST("thrust_booster", 3), EFFICIENCY("efficiency_regulator", 3), STABILIZER("hover_stabilizer", 1), PLATING("armor_plating", 2);

        public final String item;
        public final int max;

        Mod(String item, int max) {
            this.item = item;
            this.max = max;
        }
    }

    public static int level(ItemStack s, Mod m) {
        CompoundTag t = s.getTag();
        return t == null ? 0 : t.getCompound(MODS).getInt(m.name());
    }

    public static void setLevel(ItemStack s, Mod m, int lvl) {
        CompoundTag mods = s.getOrCreateTag().getCompound(MODS);
        mods.putInt(m.name(), lvl);
        s.getOrCreateTag().put(MODS, mods);
    }

    public static boolean engineOn(ItemStack s) {
        return s.getTag() == null || !s.getTag().getBoolean(ENGINE_OFF);
    }

    public static boolean hoverOn(ItemStack s) {
        return s.getTag() != null && s.getTag().getBoolean(HOVER);
    }

    public boolean canHover(ItemStack s) {
        return tier.hover || level(s, Mod.STABILIZER) > 0;
    }

    public double accel(ItemStack s) {
        return tier.accel * (1 + 0.15 * level(s, Mod.THRUST));
    }

    public double climb(ItemStack s) {
        return tier.climb * (1 + 0.12 * level(s, Mod.THRUST));
    }

    /** Usage multiplier from upgrades (thrust costs more, efficiency less). */
    public double usageFactor(ItemStack s) {
        return (1 + 0.25 * level(s, Mod.THRUST)) * Math.pow(0.8, level(s, Mod.EFFICIENCY));
    }

    // ------------------------------------------------------------------ fuel / energy

    public FluidStack fuel(ItemStack s) {
        return tier.fuel ? FluidUtil.getFluidContained(s).orElse(FluidStack.EMPTY) : FluidStack.EMPTY;
    }

    /** Stored amount as a fraction of capacity. */
    public float charge(ItemStack s) {
        if (tier.fuel) return fuel(s).getAmount() / (float) tier.capacity;
        return ItemEnergy.get(s) / (float) tier.capacity;
    }

    public boolean hasPower(ItemStack s) {
        if (tier.fuel) {
            FluidStack f = fuel(s);
            return !f.isEmpty() && JetFuels.grade(f) > 0;
        }
        return ItemEnergy.get(s) > 0;
    }

    /** Uses a fraction (1 = full thrust for one tick) of this tier's per-tick usage; false when empty. */
    public boolean consume(ItemStack s, double fraction) {
        double amount = tier.use * fraction * usageFactor(s);
        if (tier.fuel) {
            FluidStack f = fuel(s);
            double grade = JetFuels.grade(f);
            if (grade <= 0) return false;
            CompoundTag t = s.getOrCreateTag();
            double burn = t.getDouble(BURN) + amount / grade;
            int whole = (int) burn;
            t.putDouble(BURN, burn - whole);
            if (whole > 0) {
                var h = FluidUtil.getFluidHandler(s).resolve().orElse(null);
                if (h == null) return false;
                h.drain(whole, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
            return true;
        }
        int e = (int) Math.ceil(amount);
        int have = ItemEnergy.get(s);
        if (have <= 0) return false;
        ItemEnergy.set(s, Math.max(0, have - e));
        return true;
    }

    @Override
    public int getEnergyCapacity(ItemStack stack) {
        return tier.fuel ? 0 : tier.capacity;
    }

    @Override
    public int getEnergyTransfer(ItemStack stack) {
        return tier.fuel ? 0 : Math.max(1000, tier.capacity / 200);
    }

    @Override
    public boolean canExtractEnergy(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (!tier.fuel) return ItemEnergy.provider(this, stack);
        return new FluidHandlerItemStack(stack, tier.capacity) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return JetFuels.grade(fluid) > 0;
            }

            @Override
            public boolean canDrainFluidType(FluidStack fluid) {
                return true;
            }
        };
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        return Math.round(13 * Mth.clamp(charge(stack), 0, 1));
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        return tier.fuel ? 0xE07A2A : 0x3CC8F0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    /** Fill fuel jetpacks straight from a tank by using them on it. */
    @Nonnull
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext ctx) {
        if (tier.fuel && ctx.getPlayer() != null
                && FluidUtil.interactWithFluidHandler(ctx.getPlayer(), ctx.getHand(), ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace())) {
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        return super.useOn(ctx);
    }

    // ------------------------------------------------------------------ armour and looks

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.CHEST) return super.getAttributeModifiers(slot, stack);
        int armor = tier.armor + 2 * level(stack, Mod.PLATING);
        return ImmutableMultimap.of(Attributes.ARMOR, new AttributeModifier(ARMOR_ID, "Jetpack armor", armor, AttributeModifier.Operation.ADDITION));
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // the vanilla armor layer draws nothing; the jetpack model is drawn on the back by a render layer
        return "ls_jetpacks:textures/models/armor/empty.png";
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack s, @Nullable Level level, @Nonnull List<Component> tip, @Nonnull TooltipFlag flag) {
        if (tier.fuel) {
            FluidStack f = fuel(s);
            tip.add(Component.translatable("tooltip.ls_jetpacks.fuel", f.isEmpty() ? Component.translatable("tooltip.ls_jetpacks.empty") : f.getDisplayName(),
                    String.format("%,d", f.getAmount()), String.format("%,d", tier.capacity)).withStyle(ChatFormatting.GOLD));
        } else {
            tip.add(Component.translatable("tooltip.ls_jetpacks.energy", String.format("%,d", ItemEnergy.get(s)), String.format("%,d", tier.capacity))
                    .withStyle(ChatFormatting.AQUA));
        }
        tip.add(Component.translatable(engineOn(s) ? "tooltip.ls_jetpacks.engine_on" : "tooltip.ls_jetpacks.engine_off").withStyle(ChatFormatting.GRAY));
        if (canHover(s)) tip.add(Component.translatable(hoverOn(s) ? "tooltip.ls_jetpacks.hover_on" : "tooltip.ls_jetpacks.hover_off").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.ls_jetpacks.stats", String.format("%.0f", climb(s) * 20), String.format("%.0f", tier.sideMax * 20))
                .withStyle(ChatFormatting.DARK_GRAY));
        for (Mod m : Mod.values()) {
            int l = level(s, m);
            if (l > 0) tip.add(Component.translatable("item.ls_jetpacks." + m.item).append(m.max > 1 ? " " + l : "").withStyle(ChatFormatting.BLUE));
        }
        tip.add(Component.translatable("tooltip.ls_jetpacks.keys").withStyle(ChatFormatting.DARK_GRAY));
    }
}
