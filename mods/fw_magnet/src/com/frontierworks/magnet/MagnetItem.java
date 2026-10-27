package com.frontierworks.magnet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Item magnet: right-click to toggle; while active anywhere in your inventory it pulls in items and XP. */
public class MagnetItem extends Item {
    private final double range;

    public MagnetItem(Properties props, double range) {
        super(props.stacksTo(1));
        this.range = range;
    }

    public static boolean active(ItemStack s) {
        return s.hasTag() && s.getTag().getBoolean("Active");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        boolean now = !active(s);
        s.getOrCreateTag().putBoolean("Active", now);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, now ? 1.4f : 0.6f);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !active(stack) || !(entity instanceof Player) || entity.isShiftKeyDown()) return;
        Player p = (Player) entity;
        AABB box = p.getBoundingBox().inflate(range);
        Vec3 target = p.position().add(0, 0.5, 0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.hasPickUpDelay() && !e.getPersistentData().getBoolean("PreventRemoteMovement"));
        for (ItemEntity ie : items) {
            ie.setPos(target.x, target.y, target.z);
            ie.setNoPickUpDelay();
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box, Entity::isAlive)) {
            orb.setPos(target.x, target.y, target.z);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return active(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(active(stack) ? "tooltip.fw_magnet.on" : "tooltip.fw_magnet.off", (int) range).withStyle(ChatFormatting.GRAY));
    }
}
