package com.frontierworks.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Right-click: insert held stack (double-click: insert all matching from your inventory).
 * Left-click: take a stack (sneak: take one). Sneak + right-click with empty hand: lock/unlock the item type.
 */
public class BarrelBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<UUID, Long> lastClick = new HashMap<>();
    private final Supplier<? extends BlockEntityType<?>> type;
    private final int stacks;

    public BarrelBlock(Properties props, int stacks, Supplier<? extends BlockEntityType<?>> type) {
        super(props);
        this.type = type;
        this.stacks = stacks;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BarrelBE)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BarrelBE barrel = (BarrelBE) be;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && player.isShiftKeyDown()) {
            barrel.toggleLock();
            player.displayClientMessage(Component.translatable(barrel.isLocked() ? "msg.fw_storage.locked" : "msg.fw_storage.unlocked"), true);
            return InteractionResult.SUCCESS;
        }
        long now = level.getGameTime();
        Long last = lastClick.get(player.getUUID());
        lastClick.put(player.getUUID(), now);
        if (last != null && now - last < 10) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (barrel.accepts(st)) player.getInventory().setItem(i, barrel.insert(st, false));
            }
        } else if (!held.isEmpty()) {
            player.setItemInHand(hand, barrel.insert(held, false));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BarrelBE)) return;
        BarrelBE barrel = (BarrelBE) be;
        ItemStack out = barrel.extract(player.isShiftKeyDown() ? 1 : 64, false);
        if (!out.isEmpty() && !player.getInventory().add(out)) player.drop(out, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_storage.barrel", stacks).withStyle(ChatFormatting.GRAY));
        if (stack.getTag() != null && stack.getTag().contains("BlockEntityTag")) {
            int c = stack.getTag().getCompound("BlockEntityTag").getInt("Count");
            if (c > 0) tooltip.add(Component.translatable("tooltip.fw_storage.contains", c).withStyle(ChatFormatting.AQUA));
        }
    }
}
