package com.frontierworks.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Grave: right-click (as its owner) to get everything back. Breaking it spills the contents. */
public class GraveBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 3, 15, 14, 13);
    private final Supplier<? extends BlockEntityType<?>> type;

    public GraveBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props);
        this.type = type;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GraveBE)) return InteractionResult.PASS;
        GraveBE g = (GraveBE) be;
        if (!g.isOwner(player) && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("msg.fw_graves.not_yours", g.getOwnerName()), true);
            return InteractionResult.FAIL;
        }
        g.restore(player);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GraveBE) {
                for (ItemStack st : ((GraveBE) be).getItems()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st);
                }
                ((GraveBE) be).getItems().clear();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
