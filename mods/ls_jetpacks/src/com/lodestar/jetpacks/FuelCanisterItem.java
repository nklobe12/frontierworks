package com.lodestar.jetpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Carries fuel: use it on a tank to fill it up, use it in the air to refuel the jetpack you are wearing. */
public class FuelCanisterItem extends Item {
    public static final int CAPACITY = 8_000;

    public FuelCanisterItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return JetFuels.grade(fluid) > 0;
            }
        };
    }

    @Nonnull
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext ctx) {
        if (ctx.getPlayer() != null && FluidUtil.interactWithFluidHandler(ctx.getPlayer(), ctx.getHand(), ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace())) {
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack can = player.getItemInHand(hand);
        ItemStack pack = JetpackLogic.worn(player);
        if (pack.isEmpty() || !((JetpackItem) pack.getItem()).tier.fuel) return InteractionResultHolder.pass(can);
        if (!level.isClientSide) {
            IFluidHandler from = FluidUtil.getFluidHandler(can).resolve().orElse(null);
            IFluidHandler to = FluidUtil.getFluidHandler(pack).resolve().orElse(null);
            if (from != null && to != null) {
                FluidStack moved = FluidUtil.tryFluidTransfer(to, from, Integer.MAX_VALUE, true);
                if (!moved.isEmpty()) {
                    level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, player.getSoundSource(), 0.6f, 1.2f);
                    player.displayClientMessage(Component.translatable("msg.ls_jetpacks.refuelled", moved.getAmount()), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(can, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        return Math.round(13f * FluidUtil.getFluidContained(stack).map(FluidStack::getAmount).orElse(0) / CAPACITY);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        return 0xE07A2A;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tip, @Nonnull TooltipFlag flag) {
        FluidStack f = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        tip.add(Component.translatable("tooltip.ls_jetpacks.fuel", f.isEmpty() ? Component.translatable("tooltip.ls_jetpacks.empty") : f.getDisplayName(),
                String.format("%,d", f.getAmount()), String.format("%,d", CAPACITY)).withStyle(ChatFormatting.GOLD));
    }
}
