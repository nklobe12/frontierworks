package com.frontierworks.spacesuit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class OxygenGeneratorBlock extends BaseEntityBlock {
    public OxygenGeneratorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenGeneratorBE(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.OXYGEN_GENERATOR_BE.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof OxygenGeneratorBE be)) return InteractionResult.PASS;
        ItemStack suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!OxygenEvents.isSuitChest(suit)) suit = player.getItemInHand(hand);
        if (OxygenEvents.isSuitChest(suit)) {
            int have = suit.getOrCreateTag().getInt("Oxygen");
            int got = be.take(OxygenEvents.SUIT_MAX - have);
            if (got <= 0) {
                player.displayClientMessage(Component.translatable("msg.fw_spacesuit.empty"), true);
            } else {
                suit.getOrCreateTag().putInt("Oxygen", have + got);
                player.displayClientMessage(Component.translatable("msg.fw_spacesuit.refilled"), true);
            }
        } else {
            player.displayClientMessage(Component.translatable("tooltip.fw_spacesuit.generator", be.energy(), be.oxygen()), true);
        }
        return InteractionResult.CONSUME;
    }
}
