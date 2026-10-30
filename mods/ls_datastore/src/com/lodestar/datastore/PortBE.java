package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.item.AugmentItem;
import com.lodestar.datastore.menu.DeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A device that works on the inventory in front of it: importer, exporter or inventory link. Holds a nine-item filter
 * (allow or block list), a priority (links) and two speed augment slots (importers and exporters).
 */
public abstract class PortBE extends DataNodeBE implements MenuProvider, LSEntityBlock.IDropContents {
    public final SimpleContainer filter = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            PortBE.this.setChanged();
            filterChanged();
        }
    };
    public boolean whitelist;
    public int priority;
    public final ItemStackHandler augments = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof AugmentItem a && a.kind == AugmentItem.Kind.SPEED;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    protected int timer;

    protected PortBE(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean whitelist) {
        super(type, pos, state);
        this.whitelist = whitelist;
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(LSEntityBlock.FACING_ALL) ? s.getValue(LSEntityBlock.FACING_ALL) : Direction.NORTH;
    }

    @Override
    public boolean connectsOn(Direction d) {
        return d != facing();
    }

    /** The inventory in front, unless it is another datastore device. */
    @Nullable
    public IItemHandler target() {
        if (level == null) return null;
        BlockPos p = worldPosition.relative(facing());
        if (!level.isLoaded(p)) return null;
        BlockEntity be = level.getBlockEntity(p);
        if (be == null || be instanceof DataNodeBE) return null;
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, facing().getOpposite()).resolve().orElse(null);
    }

    public boolean filterEmpty() {
        return filter.isEmpty();
    }

    /** Whether the filter lets this item through (NBT is ignored). */
    public boolean passes(ItemStack s) {
        boolean listed = false;
        for (int i = 0; i < 9 && !listed; i++) listed = ItemStack.isSameItem(filter.getItem(i), s);
        return whitelist == listed;
    }

    public float speed() {
        float v = 0;
        for (int i = 0; i < augments.getSlots(); i++) if (augments.getStackInSlot(i).getItem() instanceof AugmentItem a) v += a.value();
        return v;
    }

    protected int interval() {
        return Math.max(2, Math.round(10 / (1 + speed())));
    }

    protected int batch() {
        return Math.round(8 * (1 + speed() * 2));
    }

    protected boolean paused() {
        return level.hasNeighborSignal(worldPosition);
    }

    public void targetChanged() {}

    protected void filterChanged() {}

    public boolean usesAugments() {
        return true;
    }

    public boolean usesPriority() {
        return false;
    }

    public void changePriority(int delta) {
        priority = Math.max(-9, Math.min(9, priority + delta));
        setChanged();
        DataGrid g = currentGrid();
        if (g != null) g.markSourcesDirty();
    }

    @Override
    public int upkeep() {
        return 2;
    }

    @Override
    protected boolean litNow() {
        return false;
    }

    @Override
    protected void nodeTick() {
        if (++timer < interval()) return;
        timer = 0;
        if (paused()) return;
        DataGrid g = network();
        IItemHandler h = target();
        if (g != null && h != null) work(g, h);
    }

    protected abstract void work(DataGrid g, IItemHandler h);

    @Override
    public void dropContents(Level level, BlockPos pos) {
        LSEntityBlock.IDropContents.dropHandler(level, pos, augments);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return DeviceMenu.port(id, inv, this);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        ListTag f = new ListTag();
        for (int i = 0; i < 9; i++) f.add(filter.getItem(i).save(new CompoundTag()));
        tag.put("Filter", f);
        tag.putBoolean("Allow", whitelist);
        tag.putInt("Priority", priority);
        tag.put("Augments", augments.serializeNBT());
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        ListTag f = tag.getList("Filter", 10);
        for (int i = 0; i < 9; i++) filter.setItem(i, i < f.size() ? ItemStack.of(f.getCompound(i)) : ItemStack.EMPTY);
        if (tag.contains("Allow")) whitelist = tag.getBoolean("Allow");
        priority = tag.getInt("Priority");
        if (tag.contains("Augments")) augments.deserializeNBT(tag.getCompound("Augments"));
    }
}
