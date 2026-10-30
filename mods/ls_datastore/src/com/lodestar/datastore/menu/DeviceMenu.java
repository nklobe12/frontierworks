package com.lodestar.datastore.menu;

import com.lodestar.core.item.AugmentItem;
import com.lodestar.datastore.DatastoreSetup;
import com.lodestar.datastore.DriveBayBE;
import com.lodestar.datastore.FabricatorBE;
import com.lodestar.datastore.PortBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Menu for drive bays (disk slots, priority), fabricators (card and augment slots, progress) and ports (filter, mode, priority, augments). */
public class DeviceMenu extends AbstractContainerMenu {
    public enum Kind { DRIVE, FABRICATOR, PORT }

    public static final int D_PRIORITY = 0, D_ALLOW = 1, D_LEFT = 2, D_TOTAL = 3, D_FLAGS = 4;
    public final Kind kind;
    public final BlockPos pos;
    public final BlockEntity be;
    private final int deviceSlots;
    private final int ghostSlots;
    private final ContainerData data;

    public static DeviceMenu drive(int id, Inventory inv, DriveBayBE be) {
        return new DeviceMenu(DatastoreSetup.DRIVE_MENU.get(), id, inv, Kind.DRIVE, be);
    }

    public static DeviceMenu fabricator(int id, Inventory inv, FabricatorBE be) {
        return new DeviceMenu(DatastoreSetup.FABRICATOR_MENU.get(), id, inv, Kind.FABRICATOR, be);
    }

    public static DeviceMenu port(int id, Inventory inv, PortBE be) {
        return new DeviceMenu(DatastoreSetup.PORT_MENU.get(), id, inv, Kind.PORT, be);
    }

    /** Client side factory. */
    public static DeviceMenu client(MenuType<?> type, Kind kind, int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be == null) throw new IllegalStateException("No datastore device at " + pos);
        return new DeviceMenu(type, id, inv, kind, be);
    }

    private DeviceMenu(MenuType<?> type, int id, Inventory inv, Kind kind, BlockEntity be) {
        super(type, id);
        this.kind = kind;
        this.be = be;
        this.pos = be.getBlockPos();
        int ghosts = 0;
        switch (kind) {
            case DRIVE -> {
                DriveBayBE d = (DriveBayBE) be;
                for (int i = 0; i < DriveBayBE.SLOTS; i++) addSlot(new SlotItemHandler(d.disks, i, 53 + (i % 4) * 18, 24 + (i / 4) * 18));
            }
            case FABRICATOR -> {
                FabricatorBE f = (FabricatorBE) be;
                for (int i = 0; i < 9; i++) addSlot(new SlotItemHandler(f.cards, i, 26 + (i % 3) * 18, 17 + (i / 3) * 18));
                for (int i = 0; i < 2; i++) addSlot(new SlotItemHandler(f.augments, i, 148, 26 + i * 18));
            }
            case PORT -> {
                PortBE p = (PortBE) be;
                for (int i = 0; i < 9; i++) {
                    addSlot(new Slot(p.filter, i, 44 + (i % 3) * 18, 17 + (i / 3) * 18) {
                        @Override
                        public boolean mayPickup(@Nonnull Player pl) {
                            return false;
                        }

                        @Override
                        public boolean mayPlace(@Nonnull ItemStack s) {
                            return false;
                        }
                    });
                }
                ghosts = 9;
                if (p.usesAugments()) for (int i = 0; i < 2; i++) addSlot(new SlotItemHandler(p.augments, i, 148, 26 + i * 18));
            }
        }
        this.ghostSlots = ghosts;
        this.deviceSlots = slots.size();
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        final BlockEntity b = be;
        data = new ContainerData() {
            final int[] client = new int[5];

            public int get(int i) {
                if (b.getLevel() != null && b.getLevel().isClientSide) return client[i];
                return switch (i) {
                    case D_PRIORITY -> 100 + (b instanceof DriveBayBE d ? d.priority : b instanceof PortBE p ? p.priority : 0);
                    case D_ALLOW -> b instanceof PortBE p && p.whitelist ? 1 : 0;
                    case D_LEFT -> b instanceof FabricatorBE f ? f.ticksLeft : 0;
                    case D_TOTAL -> b instanceof FabricatorBE f ? f.ticksTotal : 0;
                    case D_FLAGS -> b instanceof PortBE p ? (p.usesAugments() ? 1 : 0) | (p.usesPriority() ? 2 : 0) : b instanceof DriveBayBE ? 2 : 0;
                    default -> 0;
                };
            }

            public void set(int i, int v) {
                client[i] = v;
            }

            public int getCount() {
                return 5;
            }
        };
        addDataSlots(data);
    }

    public int data(int i) {
        return data.get(i);
    }

    public int priority() {
        return data.get(D_PRIORITY) - 100;
    }

    public boolean allow() {
        return data.get(D_ALLOW) != 0;
    }

    public boolean showsPriority() {
        return (data.get(D_FLAGS) & 2) != 0;
    }

    public boolean showsAugments() {
        return kind == Kind.FABRICATOR || (data.get(D_FLAGS) & 1) != 0;
    }

    public int deviceSlots() {
        return deviceSlots;
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id == 0 || id == 1) {
            int d = id == 0 ? -1 : 1;
            if (be instanceof DriveBayBE b) b.changePriority(d);
            if (be instanceof PortBE p && p.usesPriority()) p.changePriority(d);
            return true;
        }
        if (id == 2 && be instanceof PortBE p) {
            p.whitelist = !p.whitelist;
            p.setChanged();
            p.targetChanged();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slot, int button, @Nonnull ClickType type, @Nonnull Player player) {
        if (slot >= 0 && slot < ghostSlots) {
            ItemStack carried = getCarried();
            slots.get(slot).set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index < ghostSlots) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < deviceSlots) {
            if (!moveItemStackTo(stack, deviceSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = ghostSlots; i < deviceSlots && !stack.isEmpty(); i++) {
                if (slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) moved = true;
            }
            if (!moved && ghostSlots > 0 && !(stack.getItem() instanceof AugmentItem)) {
                for (int i = 0; i < ghostSlots; i++) if (ItemStack.isSameItem(slots.get(i).getItem(), stack)) return ItemStack.EMPTY;
                for (int i = 0; i < ghostSlots; i++) {
                    if (!slots.get(i).hasItem()) {
                        slots.get(i).set(stack.copyWithCount(1));
                        break;
                    }
                }
                return ItemStack.EMPTY;
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return stack.getCount() == copy.getCount() ? ItemStack.EMPTY : copy;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return !be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
}
