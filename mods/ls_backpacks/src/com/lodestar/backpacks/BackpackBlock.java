package com.lodestar.backpacks;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

/** A backpack standing on the ground. Use to open; sneak-use with an empty hand to pick it up. */
public class BackpackBlock extends LSEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<TierProp> TIER =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("tier", TierProp.class);

    /** Block-state mirror of {@link BackpackTier} (picks the model). */
    public enum TierProp implements net.minecraft.util.StringRepresentable {
        LEATHER, IRON, GOLD, DIAMOND, NETHERITE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static TierProp of(BackpackTier t) {
            return values()[t.ordinal()];
        }
    }

    public BackpackBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, Facing.HORIZONTAL, false);
        registerDefaultState(defaultBlockState().setValue(TIER, TierProp.LEATHER));
        noMenu();
        noBuckets();
        shape(Block.box(3, 0, 4, 13, 13, 12));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(TIER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BackpackBE be)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
                ItemStack item = be.toItem();
                be.pickedUp = true;
                level.removeBlock(pos, false);
                if (!player.addItem(item)) player.drop(item, false);
            } else if (player instanceof ServerPlayer sp) {
                BackpackMenu.openBlock(sp, be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        // dropped when broken (picking up with sneak-use removes the block entity's drop first)
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BackpackBE be && !be.pickedUp) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, be.toItem());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return level.getBlockEntity(pos) instanceof BackpackBE be ? be.toItem() : ItemStack.EMPTY;
    }
}
