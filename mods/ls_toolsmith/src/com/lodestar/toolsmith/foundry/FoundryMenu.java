package com.lodestar.toolsmith.foundry;

import com.lodestar.toolsmith.ToolsmithSetup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Foundry screen: up to 36 melting slots (one per block of room inside) and the player inventory. */
public class FoundryMenu extends AbstractContainerMenu {
    public static final int COLS = 6, TOP = 18, INV_Y = 140;
    public final FoundryControllerBE be;
    private final ContainerLevelAccess access;

    public FoundryMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (FoundryControllerBE) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public FoundryMenu(int id, Inventory inv, FoundryControllerBE be) {
        super(ToolsmithSetup.FOUNDRY_MENU.get(), id);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        for (int i = 0; i < FoundryControllerBE.SLOTS; i++) {
            final int idx = i;
            addSlot(new SlotItemHandler(be.items, i, 8 + (i % COLS) * 18, TOP + (i / COLS) * 18) {
                @Override
                public boolean mayPlace(@Nonnull ItemStack stack) {
                    return idx < FoundryMenu.this.be.usableSlots && super.mayPlace(stack);
                }

                @Override
                public boolean isActive() {
                    return idx < FoundryMenu.this.be.usableSlots || hasItem();
                }
            });
        }
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        be.select(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, be.getBlockState().getBlock());
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        int n = FoundryControllerBE.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(st, n, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < be.usableSlots && !st.isEmpty(); i++) {
                Slot s = slots.get(i);
                if (!s.hasItem() && s.mayPlace(st)) {
                    s.set(st.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
