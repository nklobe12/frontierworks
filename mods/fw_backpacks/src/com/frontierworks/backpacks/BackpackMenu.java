package com.frontierworks.backpacks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Backpack inventory; contents live in the backpack item's NBT. The open backpack's own slot is locked. */
public class BackpackMenu extends AbstractContainerMenu {
    public final int rows;
    private final int lockedSlot;
    private final ItemStack stack;
    private final ItemStackHandler handler;
    private final Player player;

    public BackpackMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readVarInt(), buf.readVarInt());
    }

    public BackpackMenu(int id, Inventory inv, int lockedSlot, int rows) {
        super(Content.BACKPACK_MENU.get(), id);
        this.rows = rows;
        this.lockedSlot = lockedSlot;
        this.player = inv.player;
        this.stack = inv.getItem(lockedSlot);
        this.handler = new ItemStackHandler(rows * 9) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack s) {
                return !(s.getItem() instanceof BackpackItem) && s.getItem().canFitInsideContainerItems();
            }

            @Override
            protected void onContentsChanged(int slot) {
                save();
            }
        };
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            invTag.putInt("Size", rows * 9);
            handler.deserializeNBT(invTag);
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new SlotItemHandler(handler, c + r * 9, 8 + c * 18, 18 + r * 18));
            }
        }
        int invY = 18 + rows * 18 + 14;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, invY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            final int idx = c;
            addSlot(new Slot(inv, c, 8 + c * 18, invY + 58) {
                @Override
                public boolean mayPickup(Player p) {
                    return idx != lockedSlot;
                }

                @Override
                public boolean mayPlace(ItemStack s) {
                    return idx != lockedSlot;
                }
            });
        }
    }

    private void save() {
        if (!player.level().isClientSide) {
            stack.getOrCreateTag().put("Inventory", handler.serializeNBT());
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player p) {
        if (type == ClickType.SWAP && button == lockedSlot) return;
        super.clicked(slotId, button, type, p);
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getInventory().getItem(lockedSlot) == stack && stack.getItem() instanceof BackpackItem;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        save();
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        int bp = rows * 9;
        if (index < bp) {
            if (!moveItemStackTo(st, bp, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(st, 0, bp, false)) {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
