package com.frontierworks.goggles;

import com.frontierworks.core.energy.IEnergyItem;
import com.frontierworks.core.energy.ItemEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

/** Powered goggles: night vision; advanced goggles also make nearby creatures glow. */
public class GogglesItem extends ArmorItem implements IEnergyItem {
    private final int capacity;
    private final boolean detect;

    public GogglesItem(ArmorMaterial mat, Properties props, int capacity, boolean detect) {
        super(mat, Type.HELMET, props.stacksTo(1));
        this.capacity = capacity;
        this.detect = detect;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player) || level.getGameTime() % 20 != 0) return;
        Player p = (Player) entity;
        if (p.getItemBySlot(EquipmentSlot.HEAD) != stack) return;
        int cost = detect ? 60 : 20;
        if (!ItemEnergy.use(stack, cost)) return;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        if (detect) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(24), e -> e instanceof Mob)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
            }
        }
    }

    @Override
    public int getEnergyCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public int getEnergyTransfer(ItemStack stack) {
        return 2000;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return ItemEnergy.provider(this, stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemEnergy.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemEnergy.barColor(stack);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemEnergy.tooltip(stack, tooltip);
        tooltip.add(Component.translatable(detect ? "tooltip.fw_goggles.detect" : "tooltip.fw_goggles.night").withStyle(ChatFormatting.GRAY));
    }
}
