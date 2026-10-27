package com.frontierworks.pipes;

import com.frontierworks.core.block.PipeBlockBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Item pipe. Extraction pipes pull from adjacent inventories and route to the nearest inventory that accepts. */
public class ItemPipeBlock extends PipeBlockBase {
    public final int rate;
    public final int interval;

    public ItemPipeBlock(Properties props, int rate, int interval, Supplier<? extends BlockEntityType<?>> type) {
        super(props, 3, type);
        this.rate = rate;
        this.interval = interval;
    }

    public boolean extracts() {
        return rate > 0;
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        if (level.getBlockState(np).getBlock() instanceof ItemPipeBlock) return true;
        BlockEntity be = level.getBlockEntity(np);
        return be != null && !(be instanceof ItemPipeBE) && be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!extracts()) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ItemPipeBE)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack held = player.getItemInHand(hand);
            ItemPipeBE pipe = (ItemPipeBE) be;
            if (held.isEmpty() && player.isShiftKeyDown()) {
                pipe.setFilter(ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("msg.fw_pipes.filter_cleared"), true);
            } else if (!held.isEmpty()) {
                pipe.setFilter(held);
                player.displayClientMessage(Component.translatable("msg.fw_pipes.filter_set", held.getHoverName()), true);
            } else {
                ItemStack f = pipe.getFilter();
                player.displayClientMessage(f.isEmpty() ? Component.translatable("msg.fw_pipes.no_filter")
                        : Component.translatable("msg.fw_pipes.filter_set", f.getHoverName()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (extracts()) {
            tooltip.add(Component.translatable("tooltip.fw_pipes.extract", rate, interval / 20.0f).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.fw_pipes.filter").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.fw_pipes.transport").withStyle(ChatFormatting.GRAY));
        }
    }
}
