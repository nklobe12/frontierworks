package com.frontierworks.backpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/** Wearable-in-inventory storage. Right-click to open. Contents stay inside the item. */
public class BackpackItem extends Item {
    private final int rows;

    public BackpackItem(Properties props, int rows) {
        super(props.stacksTo(1));
        this.rows = rows;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
            if (slot < 0) {
                player.displayClientMessage(Component.translatable("msg.fw_backpacks.main_hand"), true);
                return InteractionResultHolder.fail(stack);
            }
            final int s = slot;
            NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider((id, inv, p) -> new BackpackMenu(id, inv, s, rows), stack.getHoverName()),
                    buf -> {
                        buf.writeVarInt(s);
                        buf.writeVarInt(rows);
                    });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_backpacks.slots", rows * 9).withStyle(ChatFormatting.GRAY));
    }
}
