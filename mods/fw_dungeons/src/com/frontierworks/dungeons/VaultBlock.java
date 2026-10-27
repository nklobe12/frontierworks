package com.frontierworks.dungeons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * A locked treasure vault. Using the matching key on it consumes the key and spills out a roll of
 * the vault's loot table; the vault then stays open (empty) for good.
 */
public class VaultBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final Supplier<? extends net.minecraft.world.item.Item> key;
    private final ResourceLocation table;

    public VaultBlock(Properties props, Supplier<? extends net.minecraft.world.item.Item> key, ResourceLocation table) {
        super(props);
        this.key = key;
        this.table = table;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(OPEN)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.fw_dungeons.vault_empty").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(key.get())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("msg.fw_dungeons.vault_locked", key.get().getDescription()).withStyle(ChatFormatting.RED), true);
                level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 1.6f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel) level;
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.setBlock(pos, state.setValue(OPEN, true), 3);
            LootTable lt = sl.getServer().getLootData().getLootTable(table);
            LootParams params = new LootParams.Builder(sl)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST);
            Direction face = state.getValue(FACING);
            for (ItemStack stack : lt.getRandomItems(params)) {
                Block.popResourceFromFace(level, pos, face, stack);
            }
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 0.7f);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.2f);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5 + face.getStepX() * 0.6, pos.getY() + 0.6,
                    pos.getZ() + 0.5 + face.getStepZ() * 0.6, 12, 0.3, 0.3, 0.3, 0.0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
