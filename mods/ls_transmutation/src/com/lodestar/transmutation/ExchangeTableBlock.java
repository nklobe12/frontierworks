package com.lodestar.transmutation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/** The exchange table: dissolve items into aether and condense anything you have learned. */
public class ExchangeTableBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public ExchangeTableBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> new ExchangeMenu(id, inv, pos),
                    Component.translatable("block.ls_transmutation.exchange_table")), buf -> {
                buf.writeBoolean(true);
                buf.writeBlockPos(pos);
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
