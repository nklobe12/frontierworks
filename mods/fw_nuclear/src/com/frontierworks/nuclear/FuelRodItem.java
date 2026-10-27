package com.frontierworks.nuclear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Uranium fuel rod: burns for a fixed number of reactor ticks. Mildly radioactive when carried. */
public class FuelRodItem extends Item {
    private final int life;
    private final boolean radioactive;

    public FuelRodItem(Properties props, int life, boolean radioactive) {
        super(props.stacksTo(1));
        this.life = life;
        this.radioactive = radioactive;
    }

    public int used(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Burned") : 0;
    }

    public boolean depleted(ItemStack stack) {
        return life > 0 && used(stack) >= life;
    }

    public void burn(ItemStack stack, int ticks) {
        stack.getOrCreateTag().putInt("Burned", used(stack) + ticks);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return life > 0 && used(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return life <= 0 ? 0 : Math.round(13.0f * (life - used(stack)) / life);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x6EC54B;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (radioactive && !level.isClientSide && entity instanceof LivingEntity && level.getGameTime() % 100 == 0) {
            ((LivingEntity) entity).addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 0, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (life > 0) {
            tooltip.add(Component.translatable("tooltip.fw_nuclear.rod_life", (life - used(stack)) / 20).withStyle(ChatFormatting.GREEN));
        }
        if (radioactive) tooltip.add(Component.translatable("tooltip.fw_nuclear.radioactive").withStyle(ChatFormatting.YELLOW));
    }
}
