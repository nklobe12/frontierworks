package com.frontierworks.buildingtools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builder's Wand: right-click a block face to extend every connected matching block on that face by one,
 * using blocks from your inventory (up to the wand's limit).
 */
public class BuildersWandItem extends Item {
    private final int limit;

    public BuildersWandItem(Properties props, int limit) {
        super(props);
        this.limit = limit;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos origin = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockState target = level.getBlockState(origin);
        ItemStack wanted = target.getBlock().getCloneItemStack(level, origin, target);
        if (wanted.isEmpty()) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        List<BlockPos> places = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        int available = player.getAbilities().instabuild ? Integer.MAX_VALUE : count(player, wanted);
        while (!queue.isEmpty() && places.size() < Math.min(limit, available)) {
            BlockPos p = queue.poll();
            BlockPos dest = p.relative(face);
            if (!level.getBlockState(dest).canBeReplaced() || !level.isUnobstructed(target, dest, net.minecraft.world.phys.shapes.CollisionContext.empty())) continue;
            places.add(dest);
            for (Direction d : Direction.values()) {
                if (d.getAxis() == face.getAxis()) continue;
                BlockPos n = p.relative(d);
                if (seen.add(n) && level.getBlockState(n) == target) queue.add(n);
            }
        }
        for (BlockPos p : places) {
            level.setBlockAndUpdate(p, target);
        }
        if (!places.isEmpty()) {
            if (!player.getAbilities().instabuild) take(player, wanted, places.size());
            level.playSound(null, origin, target.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
            ctx.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
        }
        return InteractionResult.SUCCESS;
    }

    private static int count(Player p, ItemStack wanted) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(s, wanted)) n += s.getCount();
        }
        return n;
    }

    private static void take(Player p, ItemStack wanted, int amount) {
        for (int i = 0; i < p.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(s, wanted)) {
                int t = Math.min(amount, s.getCount());
                s.shrink(t);
                amount -= t;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_buildingtools.wand", limit).withStyle(ChatFormatting.GRAY));
    }
}
