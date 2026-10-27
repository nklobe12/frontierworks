package com.lodestar.ducts.transit;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Holds a station's name. Use a name tag on it (or place one renamed in an anvil) to name it. */
public class StationBE extends LSBlockEntity implements LSEntityBlock.IInteractable, LSEntityBlock.IPlacedBy {
    private String name = "";

    public StationBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String name() {
        return name.isEmpty() ? "Station " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() : name;
    }

    @Override
    protected boolean isTicking() {
        return false;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) && held.hasCustomHoverName()) {
            if (!player.level().isClientSide) {
                name = held.getHoverName().getString();
                if (!player.isCreative()) held.shrink(1);
                sync();
                player.displayClientMessage(Component.translatable("msg.ls_ducts.station_named", name), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if (player instanceof ServerPlayer sp) Transit.openDestinations(sp, worldPosition);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            name = stack.getHoverName().getString();
            sync();
        }
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.putString("Name", name);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        name = tag.getString("Name");
    }
}
