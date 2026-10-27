package com.lodestar.ducts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Nine ghost slots and a whitelist switch for a servo, filter or retriever. */
public class FilterMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final Direction side;
    @Nullable
    private final DuctBE duct;
    private final SimpleContainer ghosts = new SimpleContainer(9);
    private final ContainerData data;

    public FilterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), Direction.from3DDataValue(buf.readVarInt()), null);
    }

    public FilterMenu(int id, Inventory inv, DuctBE duct, Direction side) {
        this(id, inv, duct.getBlockPos(), side, duct);
    }

    private FilterMenu(int id, Inventory inv, BlockPos pos, Direction side, @Nullable DuctBE duct) {
        super(DuctsSetup.FILTER_MENU.get(), id);
        this.pos = pos;
        this.side = side;
        this.duct = duct;
        Attachment a = duct == null ? null : duct.attachment(side);
        if (a != null) for (int i = 0; i < 9; i++) ghosts.setItem(i, a.filter[i].copy());
        data = a == null ? new SimpleContainerData(1) : new ContainerData() {
            public int get(int i) {
                return a.whitelist ? 1 : 0;
            }

            public void set(int i, int v) {
                a.whitelist = v != 0;
            }

            public int getCount() {
                return 1;
            }
        };
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(ghosts, i, 62 + (i % 3) * 18, 17 + (i / 3) * 18) {
                @Override
                public boolean mayPickup(@Nonnull Player p) {
                    return false;
                }

                @Override
                public boolean mayPlace(@Nonnull ItemStack s) {
                    return false;
                }
            });
        }
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public boolean whitelist() {
        return data.get(0) != 0;
    }

    @Nullable
    private Attachment attachment() {
        return duct == null || duct.isRemoved() ? null : duct.attachment(side);
    }

    private void store() {
        Attachment a = attachment();
        if (a == null) return;
        for (int i = 0; i < 9; i++) a.filter[i] = ghosts.getItem(i).copy();
        duct.setChanged();
    }

    @Override
    public void clicked(int slot, int button, @Nonnull ClickType type, @Nonnull Player player) {
        if (slot >= 0 && slot < 9) {
            ItemStack carried = getCarried();
            ghosts.setItem(slot, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            store();
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id == 0) {
            Attachment a = attachment();
            if (a != null) {
                a.whitelist = !a.whitelist;
                duct.setChanged();
            }
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        if (index >= 9) {
            ItemStack s = slots.get(index).getItem();
            if (s.isEmpty()) return ItemStack.EMPTY;
            for (int i = 0; i < 9; i++) if (ItemStack.isSameItem(ghosts.getItem(i), s)) return ItemStack.EMPTY;
            for (int i = 0; i < 9; i++) {
                if (ghosts.getItem(i).isEmpty()) {
                    ghosts.setItem(i, s.copyWithCount(1));
                    store();
                    break;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (duct == null) return true;
        return attachment() != null && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
}
