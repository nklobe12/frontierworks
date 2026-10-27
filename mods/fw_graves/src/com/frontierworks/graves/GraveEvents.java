package com.frontierworks.graves;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

/** When a player dies, their drops go into a grave at the death spot instead of scattering. */
public final class GraveEvents {
    private GraveEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(GraveEvents::onDrops);
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || event.getDrops().isEmpty()) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Level level = player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        BlockPos pos = findSpot(level, player.blockPosition());
        if (pos == null) return;
        BlockState grave = Content.GRAVE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, grave);
        if (!(level.getBlockEntity(pos) instanceof GraveBE)) return;
        GraveBE be = (GraveBE) level.getBlockEntity(pos);
        List<ItemStack> items = new ArrayList<>();
        for (ItemEntity ie : event.getDrops()) items.add(ie.getItem().copy());
        be.store(player, items);
        event.setCanceled(true);
        player.sendSystemMessage(Component.translatable("msg.fw_graves.placed", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD));
    }

    private static BlockPos findSpot(Level level, BlockPos start) {
        int minY = level.getMinBuildHeight() + 1;
        BlockPos base = new BlockPos(start.getX(), Math.max(minY, Math.min(start.getY(), level.getMaxBuildHeight() - 2)), start.getZ());
        for (int r = 0; r <= 4; r++) {
            for (int dy = 0; dy <= 6; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos p = base.offset(dx, dy, dz);
                        BlockState s = level.getBlockState(p);
                        if ((s.isAir() || s.canBeReplaced()) && s.getFluidState().isEmpty() && !level.isOutsideBuildHeight(p)) return p;
                    }
                }
            }
        }
        return base.above();
    }
}
