package com.frontierworks.chunkloader;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nullable;
import java.util.List;

/** Keeps the 3x3 (or 5x5) chunks around it loaded and ticking while it exists. */
public class ChunkLoaderBlock extends Block {
    public static final String MODID = "fw_chunkloader";
    private final int radius;

    public ChunkLoaderBlock(Properties props, int radius) {
        super(props);
        this.radius = radius;
    }

    public static void init() {
        ForgeChunkManager.setForcedChunkLoadingValidationCallback(MODID, (level, helper) -> {
            for (BlockPos owner : helper.getBlockTickets().keySet()) {
                if (!(level.getBlockState(owner).getBlock() instanceof ChunkLoaderBlock)) {
                    helper.removeAllTickets(owner);
                }
            }
        });
    }

    private void force(ServerLevel level, BlockPos pos, boolean add) {
        ChunkPos c = new ChunkPos(pos);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ForgeChunkManager.forceChunk(level, MODID, pos, c.x + dx, c.z + dz, add, true);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel) force((ServerLevel) level, pos, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level instanceof ServerLevel && !old.is(this)) force((ServerLevel) level, pos, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel) force((ServerLevel) level, pos, false);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        int s = radius * 2 + 1;
        tooltip.add(Component.translatable("tooltip.fw_chunkloader.area", s, s).withStyle(ChatFormatting.GRAY));
    }
}
