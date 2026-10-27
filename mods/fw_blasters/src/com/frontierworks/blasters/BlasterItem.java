package com.frontierworks.blasters;

import com.frontierworks.core.energy.EnergyItem;
import com.frontierworks.core.energy.ItemEnergy;
import com.frontierworks.core.util.FW;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Energy weapon: each shot costs FE and fires one or more bolts. */
public class BlasterItem extends EnergyItem {
    private final int cost;
    private final float damage;
    private final int cooldown;
    private final int pellets;
    private final float spread;
    private final float explosion;
    private final String bolt;

    public BlasterItem(Properties props, int capacity, int cost, float damage, int cooldown, int pellets, float spread, float explosion, String bolt) {
        super(props, capacity, Math.max(cost * 4, capacity / 50), false);
        this.cost = cost;
        this.damage = damage;
        this.cooldown = cooldown;
        this.pellets = pellets;
        this.spread = spread;
        this.explosion = explosion;
        this.bolt = bolt;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && !ItemEnergy.has(stack, cost)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("tooltip.fw_blasters.empty"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.6f);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) ItemEnergy.use(stack, cost);
            for (int i = 0; i < pellets; i++) {
                BoltEntity b = new BoltEntity(Content.BOLT.get(), player, level, damage, explosion);
                b.setItem(FW.stack("fw_blasters:bolt_" + bolt, 1));
                float inacc = pellets > 1 ? spread * 30f : 0.2f;
                b.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.2f, inacc);
                level.addFreshEntity(b);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), explosion > 0 ? SoundEvents.FIRECHARGE_USE : SoundEvents.SHULKER_SHOOT,
                    SoundSource.PLAYERS, 0.6f, explosion > 0 ? 0.6f : 1.8f);
        }
        player.getCooldowns().addCooldown(this, cooldown);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
