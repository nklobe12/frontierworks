package com.lodestar.transmutation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exchange table / tablet. Items put in the dissolve slot are learned and turned into aether; known items are
 * bought back from the grid on the screen (sent to the server as a "withdraw" action). An aether cell in the
 * cell slot can be filled from or emptied into your stored aether.
 */
public class ExchangeMenu extends AbstractContainerMenu {
    public static final int W = 230, H = 222;
    public static final int BUTTON_FILL_CELL = 0, BUTTON_EMPTY_CELL = 1;
    private final SimpleContainer slots2 = new SimpleContainer(2);
    @Nullable
    private final BlockPos table;
    private final Player player;

    public ExchangeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBoolean() ? buf.readBlockPos() : null);
    }

    public ExchangeMenu(int id, Inventory inv, @Nullable BlockPos table) {
        super(TransmutationSetup.EXCHANGE_MENU.get(), id);
        this.table = table;
        this.player = inv.player;
        addSlot(new Slot(slots2, 0, 18, 34) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return AetherValues.has(stack) && !(stack.getItem() instanceof AetherCellItem);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                dissolve();
            }
        });
        addSlot(new Slot(slots2, 1, 18, 82) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.getItem() instanceof AetherCellItem;
            }
        });
        int ix = (W - 162) / 2;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, ix + c * 18, 140 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, ix + c * 18, 198));
    }

    private void dissolve() {
        if (!(player instanceof ServerPlayer sp)) return;
        ItemStack st = slots2.getItem(0);
        if (st.isEmpty()) return;
        long v = AetherValues.get(st.copyWithCount(1));
        if (v <= 0) return;
        Knowledge.learn(sp, st.getItem());
        Knowledge.setAether(sp, Knowledge.aether(sp) + v * st.getCount());
        slots2.setItem(0, ItemStack.EMPTY);
        Knowledge.sync(sp);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return true;
        ItemStack cell = slots2.getItem(1);
        if (!(cell.getItem() instanceof AetherCellItem c)) return true;
        if (id == BUTTON_FILL_CELL) {
            long put = AetherCellItem.charge(cell, Knowledge.aether(sp));
            Knowledge.setAether(sp, Knowledge.aether(sp) - put);
        } else if (id == BUTTON_EMPTY_CELL) {
            long have = AetherCellItem.stored(cell);
            AetherCellItem.set(cell, 0);
            Knowledge.setAether(sp, Knowledge.aether(sp) + have);
        }
        slots2.setChanged();
        Knowledge.sync(sp);
        return true;
    }

    @Override
    public void removed(@Nonnull Player p) {
        super.removed(p);
        clearContainer(p, slots2);
    }

    @Override
    public boolean stillValid(Player p) {
        return table == null || p.distanceToSqr(table.getX() + 0.5, table.getY() + 0.5, table.getZ() + 0.5) < 64;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        if (index < 2) {
            if (!moveItemStackTo(st, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (st.getItem() instanceof AetherCellItem) {
            if (!moveItemStackTo(st, 1, 2, false)) return ItemStack.EMPTY;
        } else if (AetherValues.has(st)) {
            if (!moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
