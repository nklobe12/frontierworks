package com.frontierworks.core.machine;

import com.frontierworks.core.CoreRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** The single menu type shared by every Frontierworks machine; slots come from the machine's layout. */
public class MachineMenu extends AbstractContainerMenu {
    public final MachineBlockEntity be;
    private final ContainerLevelAccess access;
    private final int machineSlots;
    private final int[] data;

    public MachineMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, lookup(inv, buf.readBlockPos()));
    }

    private static MachineBlockEntity lookup(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineBlockEntity) {
            return (MachineBlockEntity) be;
        }
        throw new IllegalStateException("No Frontierworks machine at " + pos);
    }

    public MachineMenu(int id, Inventory inv, MachineBlockEntity be) {
        super(CoreRegistry.MACHINE_MENU.get(), id);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        MachineLayout layout = be.getLayout();
        for (int i = 0; i < layout.slots.size(); i++) {
            MachineLayout.SlotDef d = layout.slots.get(i);
            addSlot(new MachineSlot(be.getItems(), i, d.x, d.y, d.role));
        }
        this.machineSlots = layout.slots.size();
        int invY = layout.invY;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, invY + 58));
        }
        int n = be.dataCount();
        this.data = new int[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return MachineMenu.this.be.getData(idx) & 0xFFFF;
                }

                @Override
                public void set(int value) {
                    data[idx] = (data[idx] & 0xFFFF0000) | (value & 0xFFFF);
                }
            });
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (MachineMenu.this.be.getData(idx) >>> 16) & 0xFFFF;
                }

                @Override
                public void set(int value) {
                    data[idx] = (data[idx] & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                }
            });
        }
    }

    /** Synced value (client) — see MachineBlockEntity.D_* indices. */
    public int data(int i) {
        return i >= 0 && i < data.length ? data[i] : 0;
    }

    public int extra(int i) {
        return data(MachineBlockEntity.D_TANKS + be.getTanks().length * 2 + i);
    }

    public int tankAmount(int t) {
        return data(MachineBlockEntity.D_TANKS + t * 2);
    }

    public int tankFluidId(int t) {
        return data(MachineBlockEntity.D_TANKS + t * 2 + 1);
    }

    public int machineSlotCount() {
        return machineSlots;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        be.onButton(id, player);
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
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = machineSlots;
        int hotbarStart = machineSlots + 27;
        int end = slots.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, invStart, end, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
                Slot s = slots.get(i);
                if (s.mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                if (index < hotbarStart) {
                    if (!moveItemStackTo(stack, hotbarStart, end, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, invStart, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    public static class MachineSlot extends SlotItemHandler {
        public final MachineLayout.Role role;

        public MachineSlot(IItemHandler handler, int index, int x, int y, MachineLayout.Role role) {
            super(handler, index, x, y);
            this.role = role;
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return role != MachineLayout.Role.OUTPUT && super.mayPlace(stack);
        }
    }
}
