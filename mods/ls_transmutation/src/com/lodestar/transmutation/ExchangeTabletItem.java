package com.lodestar.transmutation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** A portable exchange table. */
public class ExchangeTabletItem extends Item {
    public ExchangeTabletItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> new ExchangeMenu(id, inv, (net.minecraft.core.BlockPos) null),
                    Component.translatable("item.ls_transmutation.exchange_tablet")), buf -> buf.writeBoolean(false));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
