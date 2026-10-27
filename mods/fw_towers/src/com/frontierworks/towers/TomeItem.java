package com.frontierworks.towers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Single-use wizard tome: a chunk of experience plus one random beneficial effect for three minutes. */
public class TomeItem extends Item {
    private static final MobEffect[] EFFECTS = {MobEffects.REGENERATION, MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED,
            MobEffects.NIGHT_VISION, MobEffects.FIRE_RESISTANCE, MobEffects.DAMAGE_RESISTANCE, MobEffects.JUMP, MobEffects.LUCK};

    public TomeItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.giveExperiencePoints(40 + level.random.nextInt(40));
            MobEffect effect = EFFECTS[level.random.nextInt(EFFECTS.length)];
            player.addEffect(new MobEffectInstance(effect, 20 * 180, level.random.nextInt(4) == 0 ? 1 : 0));
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("msg.fw_towers.tome").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
