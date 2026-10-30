package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** Access terminal (and crafting terminal, which keeps its 3x3 grid here). */
public class TerminalBE extends DataNodeBE implements LSEntityBlock.IInteractable, LSEntityBlock.IDropContents {
    public final boolean crafting;
    public final NonNullList<ItemStack> matrix = NonNullList.withSize(9, ItemStack.EMPTY);

    public TerminalBE(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean crafting) {
        super(type, pos, state);
        this.crafting = crafting;
    }

    @Override
    public int upkeep() {
        return crafting ? 3 : 2;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> new TerminalMenu(id, inv, this),
                    getBlockState().getBlock().getName()), buf -> TerminalMenu.writeOpen(buf, crafting, false));
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack s : matrix) if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        matrix.clear();
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        if (crafting) ContainerHelper.saveAllItems(tag, matrix);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        matrix.clear();
        if (crafting) ContainerHelper.loadAllItems(tag, matrix);
    }

    @Override
    protected void saveSync(CompoundTag tag) {}

    @Override
    protected void loadSync(CompoundTag tag) {}

    public Component title() {
        return getBlockState().getBlock().getName();
    }
}
