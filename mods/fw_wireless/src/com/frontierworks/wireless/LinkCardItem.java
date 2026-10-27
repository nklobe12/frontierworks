package com.frontierworks.wireless;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

/** Stores a transmitter location (or any block position for other Frontierworks devices). */
public class LinkCardItem extends Item {
    public LinkCardItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Nullable
    public static BlockPos getPos(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.contains("LinkPos") ? BlockPos.of(t.getLong("LinkPos")) : null;
    }

    @Nullable
    public static ResourceLocation getDim(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.contains("LinkDim") ? new ResourceLocation(t.getString("LinkDim")) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (!(be instanceof WirelessBE)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            WirelessBE w = (WirelessBE) be;
            if (w.isTransmitter()) {
                CompoundTag t = stack.getOrCreateTag();
                t.putLong("LinkPos", ctx.getClickedPos().asLong());
                t.putString("LinkDim", WirelessBE.dimOf(level).toString());
                if (player != null) player.displayClientMessage(Component.translatable("msg.fw_wireless.stored"), true);
            } else {
                BlockPos p = getPos(stack);
                ResourceLocation d = getDim(stack);
                if (p != null && d != null) {
                    w.link(p, d);
                    if (player != null) player.displayClientMessage(Component.translatable("msg.fw_wireless.linked"), true);
                } else if (player != null) {
                    player.displayClientMessage(Component.translatable("msg.fw_wireless.empty"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BlockPos p = getPos(stack);
        if (p != null) {
            tooltip.add(Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ() + " @ " + getDim(stack)).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getPos(stack) != null;
    }
}
