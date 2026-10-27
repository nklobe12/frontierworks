package com.lodestar.toolsmith.station;

import com.lodestar.toolsmith.Materials;
import com.lodestar.toolsmith.PartItem;
import com.lodestar.toolsmith.PartType;
import com.lodestar.toolsmith.ToolMaterial;
import com.lodestar.toolsmith.ToolsmithSetup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Part builder: a blank pattern (kept) and a material (consumed by the part's cost). Pick the part on the left;
 * the result slot shows it and taking it uses up the material.
 */
public class PartBuilderMenu extends AbstractContainerMenu {
    public final StationBE be;
    private final ContainerLevelAccess access;
    private final Container result = new SimpleContainer(1);
    private final DataSlot selected = DataSlot.standalone();

    public PartBuilderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (StationBE) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public PartBuilderMenu(int id, Inventory inv, StationBE be) {
        super(ToolsmithSetup.PART_BUILDER_MENU.get(), id);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        addSlot(new SlotItemHandler(be.items, 0, 112, 18) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.is(ToolsmithSetup.blankPattern());
            }

            @Override
            public void setChanged() {
                super.setChanged();
                updateResult();
            }
        });
        addSlot(new SlotItemHandler(be.items, 1, 112, 50) {
            @Override
            public void setChanged() {
                super.setChanged();
                updateResult();
            }
        });
        addSlot(new Slot(result, 0, 150, 34) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@Nonnull Player player, @Nonnull ItemStack stack) {
                PartType p = PartType.values()[selected.get()];
                be.items.extractItem(1, p.cost, false);
                updateResult();
                super.onTake(player, stack);
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        selected.set(be.selected);
        addDataSlot(selected);
        updateResult();
    }

    public int selected() {
        return selected.get();
    }

    @Nullable
    public static ToolMaterial materialOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (ToolMaterial m : Materials.all()) {
            if (m.craftable() && m.ingredient() != null && m.ingredient().test(stack)) return m;
        }
        return null;
    }

    private void updateResult() {
        ItemStack pattern = be.items.getStackInSlot(0);
        ItemStack mat = be.items.getStackInSlot(1);
        ToolMaterial m = materialOf(mat);
        PartType p = PartType.values()[Math.max(0, Math.min(PartType.values().length - 1, selected.get()))];
        if (!pattern.isEmpty() && m != null && mat.getCount() >= p.cost) result.setItem(0, PartItem.of(p, m));
        else result.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < PartType.values().length) {
            selected.set(id);
            be.selected = id;
            be.setChanged();
            updateResult();
        }
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
        if (index == 2) {
            // craft as many as the material allows
            ItemStack made = ItemStack.EMPTY;
            for (int i = 0; i < 64 && slot.hasItem(); i++) {
                ItemStack one = slot.getItem().copy();
                if (!moveItemStackTo(one, 3, slots.size(), true)) break;
                slot.onTake(player, one);
                made = one;
            }
            return made.isEmpty() ? ItemStack.EMPTY : copy;
        }
        if (index < 3) {
            if (!moveItemStackTo(st, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (st.is(ToolsmithSetup.blankPattern())) {
            if (!moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(st, 1, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        updateResult();
        return copy;
    }
}
