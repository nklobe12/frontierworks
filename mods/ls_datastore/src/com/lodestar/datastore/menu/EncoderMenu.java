package com.lodestar.datastore.menu;

import com.lodestar.datastore.DatastoreSetup;
import com.lodestar.datastore.EncoderBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Nine ghost slots for the recipe, the blank card input, the encoded card output, a substitutes switch and Encode. */
public class EncoderMenu extends AbstractContainerMenu {
    public static final int GHOSTS = 9, CARD_IN = 9, CARD_OUT = 10, PLAYER = 11;
    public final BlockPos pos;
    public final EncoderBE be;
    private final ContainerData data;

    public EncoderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, lookup(inv, buf.readBlockPos()));
    }

    private static EncoderBE lookup(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof EncoderBE e) return e;
        throw new IllegalStateException("No pattern encoder at " + pos);
    }

    public EncoderMenu(int id, Inventory inv, EncoderBE be) {
        super(DatastoreSetup.ENCODER_MENU.get(), id);
        this.be = be;
        this.pos = be.getBlockPos();
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(be.grid, i, 30 + (i % 3) * 18, 17 + (i / 3) * 18) {
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
        addSlot(new SlotItemHandler(be.cards, 0, 152, 17));
        addSlot(new SlotItemHandler(be.cards, 1, 152, 53) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack s) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        data = new ContainerData() {
            int client;

            public int get(int i) {
                return be.getLevel() != null && be.getLevel().isClientSide ? client : be.substitute ? 1 : 0;
            }

            public void set(int i, int v) {
                client = v;
            }

            public int getCount() {
                return 1;
            }
        };
        addDataSlots(data);
    }

    public boolean substitute() {
        return data.get(0) != 0;
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id == 0) {
            be.encode();
            return true;
        }
        if (id == 1) {
            be.substitute = !be.substitute;
            be.setChanged();
            return true;
        }
        if (id == 2) {
            for (int i = 0; i < 9; i++) be.grid.setItem(i, ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slot, int button, @Nonnull ClickType type, @Nonnull Player player) {
        if (slot >= 0 && slot < GHOSTS) {
            ItemStack carried = getCarried();
            be.grid.setItem(slot, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (index < GHOSTS || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER) {
            if (!moveItemStackTo(stack, PLAYER, slots.size(), true)) return ItemStack.EMPTY;
        } else if (slots.get(CARD_IN).mayPlace(stack)) {
            if (!moveItemStackTo(stack, CARD_IN, CARD_IN + 1, false)) return ItemStack.EMPTY;
        } else {
            for (int i = 0; i < GHOSTS; i++) {
                if (!slots.get(i).hasItem()) {
                    be.grid.setItem(i, stack.copyWithCount(1));
                    break;
                }
            }
            return ItemStack.EMPTY;
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
