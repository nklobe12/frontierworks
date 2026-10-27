package com.frontierworks.sorting;

import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import com.frontierworks.core.net.CoreNet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server side of inventory sorting: merges stacks and sorts by item id. */
public final class Sorter {
    private Sorter() {}

    public static void init() {
        CoreNet.register("fw_sorting:sort", (player, target) -> {
            if (target == 1) sortPlayer(player);
            else sortContainer(player);
        });
    }

    private static void sortPlayer(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<Slot> slots = new ArrayList<>();
        AbstractContainerMenu menu = player.inventoryMenu;
        for (Slot s : menu.slots) {
            if (s.container == inv && s.getContainerSlot() >= 9 && s.getContainerSlot() < 36) slots.add(s);
        }
        sort(slots);
        player.inventoryMenu.broadcastChanges();
    }

    private static void sortContainer(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        List<Slot> slots = new ArrayList<>();
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu || menu instanceof DispenserMenu) {
            for (Slot s : menu.slots) {
                if (!(s.container instanceof Inventory)) slots.add(s);
            }
        } else if (menu instanceof MachineMenu) {
            MachineMenu mm = (MachineMenu) menu;
            for (int i = 0; i < mm.machineSlotCount(); i++) {
                Slot s = mm.slots.get(i);
                if (s instanceof MachineMenu.MachineSlot && ((MachineMenu.MachineSlot) s).role == MachineLayout.Role.STORAGE) slots.add(s);
            }
        } else {
            sortPlayer(player);
            return;
        }
        sort(slots);
        menu.broadcastChanges();
    }

    private static void sort(List<Slot> slots) {
        List<ItemStack> items = new ArrayList<>();
        for (Slot s : slots) {
            ItemStack st = s.getItem();
            if (!st.isEmpty()) items.add(st.copy());
        }
        // merge
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack st : items) {
            for (ItemStack m : merged) {
                if (st.isEmpty()) break;
                if (ItemStack.isSameItemSameTags(m, st) && m.getCount() < m.getMaxStackSize()) {
                    int move = Math.min(st.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(move);
                    st.shrink(move);
                }
            }
            if (!st.isEmpty()) merged.add(st);
        }
        merged.sort(Comparator.comparing((ItemStack st) -> BuiltInRegistries.ITEM.getKey(st.getItem()).toString())
                .thenComparing(st -> -st.getCount()));
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).set(i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
        }
    }

    static boolean isContainer(Container c) {
        return !(c instanceof Inventory);
    }
}
