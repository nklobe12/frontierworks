package com.lodestar.kinetics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Use on one pulley, then on another on a parallel axle, to fit a belt between them. */
public class DriveBeltItem extends Item {
    public DriveBeltItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof PulleyBE pulley)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        CompoundTag tag = stack.getTag();
        BlockPos first = tag != null && tag.contains("First") ? NbtUtils.readBlockPos(tag.getCompound("First")) : null;
        String dim = tag != null ? tag.getString("Dim") : "";
        if (first == null || !dim.equals(level.dimension().location().toString())) {
            if (pulley.partner() != null) {
                msg(player, "msg.ls_kinetics.belt_taken", ChatFormatting.RED);
                return InteractionResult.FAIL;
            }
            CompoundTag t = stack.getOrCreateTag();
            t.put("First", NbtUtils.writeBlockPos(ctx.getClickedPos()));
            t.putString("Dim", level.dimension().location().toString());
            msg(player, "msg.ls_kinetics.belt_first", ChatFormatting.YELLOW);
            return InteractionResult.SUCCESS;
        }
        stack.removeTagKey("First");
        stack.removeTagKey("Dim");
        if (first.equals(ctx.getClickedPos())) {
            msg(player, "msg.ls_kinetics.belt_cancel", ChatFormatting.GRAY);
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(first) instanceof PulleyBE a)) {
            msg(player, "msg.ls_kinetics.belt_lost", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        String why = a.canLink(pulley);
        if (why != null) {
            msg(player, why, ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        a.link(pulley);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, ctx.getClickedPos(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 1f, 1f);
        if (player != null) player.displayClientMessage(Component.translatable("msg.ls_kinetics.belt_done",
                Math.round(Math.sqrt(first.distSqr(ctx.getClickedPos())))).withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    private static void msg(@Nullable Player p, String key, ChatFormatting style) {
        if (p != null) p.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("First");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("First")) {
            BlockPos p = NbtUtils.readBlockPos(stack.getTag().getCompound("First"));
            tip.add(Component.translatable("tooltip.ls_kinetics.belt_from", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.YELLOW));
        }
    }
}
