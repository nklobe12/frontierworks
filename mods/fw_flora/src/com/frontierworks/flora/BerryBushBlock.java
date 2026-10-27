package com.frontierworks.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/** A berry bush (age 0-3) that does not prick: right-click when ripe to pick berries; bone meal speeds it up. */
public class BerryBushBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape SMALL = Block.box(3, 0, 3, 13, 8, 13);
    private static final VoxelShape BIG = Block.box(1, 0, 1, 15, 16, 15);
    private final Supplier<? extends Item> berry;

    public BerryBushBlock(Properties props, Supplier<? extends Item> berry) {
        super(props);
        this.berry = berry;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(berry.get());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AGE) == 0 ? SMALL : BIG;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 3;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 3 && level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(5) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age < 3 && player.getItemInHand(hand).is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
        }
        if (age > 1) {
            int n = 1 + level.random.nextInt(2) + (age == 3 ? 1 : 0);
            popResource(level, pos, new ItemStack(berry.get(), n));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f);
            level.setBlock(pos, state.setValue(AGE, 1), 2);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        return state.getValue(AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE) + 1)), 2);
    }
}
