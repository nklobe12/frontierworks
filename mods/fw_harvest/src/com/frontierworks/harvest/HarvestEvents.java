package com.frontierworks.harvest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/** Right-click a fully grown crop to harvest it and replant automatically. */
public final class HarvestEvents {
    private HarvestEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(HarvestEvents::onUse);
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState st = level.getBlockState(pos);
        Block b = st.getBlock();
        BlockState replant = null;
        if (b instanceof CropBlock && ((CropBlock) b).isMaxAge(st)) {
            replant = ((CropBlock) b).getStateForAge(0);
        } else if (b instanceof NetherWartBlock && st.getValue(NetherWartBlock.AGE) >= 3) {
            replant = st.setValue(NetherWartBlock.AGE, 0);
        } else if (b instanceof CocoaBlock && st.getValue(CocoaBlock.AGE) >= 2) {
            replant = st.setValue(CocoaBlock.AGE, 0);
        }
        if (replant == null) return;
        if (level instanceof ServerLevel) {
            ItemStack seed = b.getCloneItemStack(level, pos, st);
            List<ItemStack> drops = Block.getDrops(st, (ServerLevel) level, pos, null, player, player.getMainHandItem());
            boolean removed = false;
            for (ItemStack d : drops) {
                if (!removed && ItemStack.isSameItem(d, seed)) {
                    d.shrink(1);
                    removed = true;
                }
                if (!d.isEmpty()) Block.popResource(level, pos, d);
            }
            level.setBlock(pos, replant, 3);
            level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1f, 1f);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
