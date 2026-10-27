package com.frontierworks.explosives;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DetonatorItem extends Item {
    public DetonatorItem(Properties props) {
        super(props);
    }

    private static ListTag links(ItemStack stack) {
        return stack.getOrCreateTag().getList("Links", Tag.TAG_COMPOUND);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (p == null || !p.isShiftKeyDown() || !(level.getBlockState(ctx.getClickedPos()).getBlock() instanceof C4Block)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack stack = ctx.getItemInHand();
            ListTag list = links(stack);
            CompoundTag pos = NbtUtils.writeBlockPos(ctx.getClickedPos());
            if (!list.contains(pos)) list.add(pos);
            stack.getOrCreateTag().put("Links", list);
            p.displayClientMessage(Component.translatable("msg.fw_explosives.linked", list.size()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            ListTag list = links(stack);
            if (list.isEmpty()) {
                player.displayClientMessage(Component.translatable("msg.fw_explosives.none"), true);
            } else {
                player.displayClientMessage(Component.translatable("msg.fw_explosives.boom", list.size()), true);
                for (int i = 0; i < list.size(); i++) {
                    BlockPos pos = NbtUtils.readBlockPos(list.getCompound(i));
                    if (pos.closerThan(player.blockPosition(), 128) && level.isLoaded(pos)) C4Block.detonate(level, pos);
                }
                stack.getOrCreateTag().put("Links", new ListTag());
            }
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_explosives.links", links(stack).size()));
    }
}
