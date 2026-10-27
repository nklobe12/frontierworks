package com.frontierworks.teleporters;

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

/** Sneak-right-click a pad to record it; right-click another pad to link that pad to the recorded one. */
public class CoordinateCardItem extends Item {
    public CoordinateCardItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof TeleportPadBE)) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            if (player != null && player.isShiftKeyDown()) {
                tag.putLong("Pos", ctx.getClickedPos().asLong());
                tag.putString("Dim", level.dimension().location().toString());
                player.displayClientMessage(Component.translatable("msg.fw_teleporters.recorded"), true);
            } else if (tag.contains("Pos")) {
                ((TeleportPadBE) be).link(BlockPos.of(tag.getLong("Pos")), new ResourceLocation(tag.getString("Dim")));
                if (player != null) player.displayClientMessage(Component.translatable("msg.fw_teleporters.linked"), true);
            } else if (player != null) {
                player.displayClientMessage(Component.translatable("msg.fw_teleporters.empty"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag t = stack.getTag();
        if (t != null && t.contains("Pos")) {
            BlockPos p = BlockPos.of(t.getLong("Pos"));
            tooltip.add(Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" + t.getString("Dim") + ")").withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("Pos");
    }
}
