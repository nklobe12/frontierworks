package com.frontierworks.fluidducts;

import com.frontierworks.core.block.PipeBlockBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Fluid duct. Pump ducts pull fluid from adjacent tanks/machines and push it to every other fluid handler on the network. */
public class DuctBlock extends PipeBlockBase {
    public final int rate;

    public DuctBlock(Properties props, int rate, Supplier<? extends BlockEntityType<?>> type) {
        super(props, 3, type);
        this.rate = rate;
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        if (level.getBlockState(np).getBlock() instanceof DuctBlock) return true;
        BlockEntity be = level.getBlockEntity(np);
        return be != null && !(be instanceof DuctBE) && be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((rate > 0 ? Component.translatable("tooltip.fw_fluidducts.pump", rate) : Component.translatable("tooltip.fw_fluidducts.duct"))
                .withStyle(ChatFormatting.GRAY));
    }
}
