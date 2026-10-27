package com.frontierworks.rocketry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class RocketItem extends Item {
    private final int tier;

    public RocketItem(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!level.getBlockState(pos).is(Content.LAUNCH_PAD.get())) {
            if (player != null && !level.isClientSide) player.displayClientMessage(Component.translatable("msg.fw_rocketry.pad"), true);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            RocketEntity r = Content.ROCKET.get().create(level);
            if (r == null) return InteractionResult.FAIL;
            r.setTier(tier);
            r.setFuel(ctx.getItemInHand().getOrCreateTag().getInt("Fuel"));
            r.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ctx.getRotation(), 0);
            level.addFreshEntity(r);
            if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int f = stack.hasTag() ? stack.getTag().getInt("Fuel") : 0;
        tooltip.add(Component.translatable("tooltip.fw_rocketry.fuel", f, RocketEntity.fuelNeeded(tier)));
    }
}
