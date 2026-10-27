package com.lodestar.metalwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Pump by hand or with a redstone pulse to blow air into the forge or bloomery it faces: hotter, faster, hungrier. */
public class BellowsBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public BellowsBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) pump(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
            if (powered && !level.isClientSide) pump(level, pos, state);
        }
    }

    private static void pump(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos target = pos.relative(f);
        level.playSound(null, pos, SoundEvents.PHANTOM_FLAP, SoundSource.BLOCKS, 0.6f, 0.6f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5 + f.getStepX() * 0.6, pos.getY() + 0.5, pos.getZ() + 0.5 + f.getStepZ() * 0.6,
                    3, 0.05, 0.05, 0.05, 0.02);
        }
        if (level.getBlockEntity(target) instanceof ForgeBE forge) forge.blow();
        if (level.getBlockEntity(target) instanceof BloomeryBE bloomery) bloomery.blow();
    }
}
