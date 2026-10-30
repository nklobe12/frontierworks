package com.lodestar.datastore.menu;

import com.lodestar.datastore.CoreBE;
import com.lodestar.datastore.DatastoreSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Status screen of a core; the numbers come from the core's synced block entity data. */
public class CoreMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    @Nullable
    private final CoreBE be;

    public CoreMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(DatastoreSetup.CORE_MENU.get(), id);
        this.pos = buf.readBlockPos();
        this.be = null;
    }

    public CoreMenu(int id, Inventory inv, CoreBE be) {
        super(DatastoreSetup.CORE_MENU.get(), id);
        this.pos = be.getBlockPos();
        this.be = be;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64);
    }
}
