package com.frontierworks.waystones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Right-click to activate (first time) and to open the travel menu listing every waystone you've activated. */
public class WaystoneBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    private final Supplier<? extends BlockEntityType<?>> type;

    public WaystoneBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
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
        if (!(be instanceof WaystoneBE) || !(player instanceof ServerPlayer)) return InteractionResult.PASS;
        WaystoneBE w = (WaystoneBE) be;
        String dim = level.dimension().location().toString();
        if (WaystoneData.add(player, pos, dim, w.getName())) {
            player.displayClientMessage(Component.translatable("msg.fw_waystones.activated", w.getName()).withStyle(ChatFormatting.AQUA), false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1f);
            ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.5, 0.4, 0.5);
            return InteractionResult.SUCCESS;
        }
        final List<WaystoneData.Entry> list = WaystoneData.list(player);
        NetworkHooks.openScreen((ServerPlayer) player,
                new SimpleMenuProvider((id, inv, p) -> new WaystoneMenu(id, inv, pos, list), Component.literal(w.getName())),
                buf -> WaystoneMenu.write(buf, pos, list));
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
    }

    public static CompoundTag nameTag(String name) {
        CompoundTag t = new CompoundTag();
        t.putString("WaystoneName", name);
        return t;
    }
}
