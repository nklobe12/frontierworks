package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.datastore.menu.DeviceMenu;
import com.lodestar.datastore.storage.DiskData;
import com.lodestar.datastore.storage.DiskStorage;
import com.lodestar.datastore.storage.IStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Eight data disk slots. Each disk is one storage of the network, at the bay's priority. */
public class DriveBayBE extends DataNodeBE implements MenuProvider, LSEntityBlock.IDropContents {
    public static final int SLOTS = 8;
    public final ItemStackHandler disks = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof DiskItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            summaryDirty = true;
            DataGrid g = currentGrid();
            if (g != null) g.markSourcesDirty();
        }
    };
    public int priority;
    private boolean summaryDirty = true;
    /** per slot, for the renderer: 0 = empty, else bits 0-2 tier + 1, bits 4-5 fill (0 ok, 1 nearly full, 2 full), bit 8 online */
    public final int[] visual = new int[SLOTS];

    public DriveBayBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int upkeep() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) if (!disks.getStackInSlot(i).isEmpty()) n++;
        return 2 + n;
    }

    /** Adds a storage for every disk to the list (assigning disk ids on first use). */
    public void storages(List<IStorage> out) {
        if (level == null || level.getServer() == null) return;
        DiskData data = DiskData.get(level.getServer());
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = disks.getStackInSlot(i);
            if (!(s.getItem() instanceof DiskItem tier)) continue;
            UUID id = DiskItem.id(s);
            if (id == null) {
                id = DiskItem.ensureId(s);
                setChanged();
            }
            out.add(new DiskStorage(data.inv(id), tier, priority, () -> summaryDirty = true));
        }
    }

    public void changePriority(int delta) {
        priority = Math.max(-9, Math.min(9, priority + delta));
        setChanged();
        DataGrid g = currentGrid();
        if (g != null) g.markSourcesDirty();
    }

    @Override
    protected void nodeTick() {
        super.nodeTick();
        if (level.getGameTime() % 20 != (worldPosition.asLong() & 15)) return;
        boolean online = litNow();
        boolean changed = false;
        if (summaryDirty && level.getServer() != null) {
            summaryDirty = false;
            DiskData data = DiskData.get(level.getServer());
            for (int i = 0; i < SLOTS; i++) {
                ItemStack s = disks.getStackInSlot(i);
                UUID id = DiskItem.id(s);
                if (id != null && DiskItem.updateSummary(s, data.inv(id))) changed = true;
            }
            if (changed) setChanged();
        }
        for (int i = 0; i < SLOTS; i++) {
            int v = visualOf(disks.getStackInSlot(i), online);
            if (v != visual[i]) {
                visual[i] = v;
                changed = true;
            }
        }
        if (changed) sync();
    }

    private static int visualOf(ItemStack s, boolean online) {
        if (!(s.getItem() instanceof DiskItem d)) return 0;
        int tier = Math.max(0, Math.min(3, Long.numberOfTrailingZeros(Math.max(1, d.bytes / 4096)) / 2));
        float f = DiskItem.fill(s);
        int fill = f >= 1f ? 2 : f > 0.75f ? 1 : 0;
        return (tier + 1) | (fill << 4) | (online ? 256 : 0);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        LSEntityBlock.IDropContents.dropHandler(level, pos, disks);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return DeviceMenu.drive(id, inv, this);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.put("Disks", disks.serializeNBT());
        tag.putInt("Priority", priority);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        if (tag.contains("Disks")) disks.deserializeNBT(tag.getCompound("Disks"));
        priority = tag.getInt("Priority");
        summaryDirty = true;
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        tag.putIntArray("Vis", visual);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        int[] v = tag.getIntArray("Vis");
        for (int i = 0; i < SLOTS; i++) visual[i] = i < v.length ? v[i] : 0;
    }
}
