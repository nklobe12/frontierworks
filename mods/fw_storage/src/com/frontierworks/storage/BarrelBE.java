package com.frontierworks.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Bulk barrel: one item type, many stacks. Shows the item and count on its face. */
public class BarrelBE extends BlockEntity {
    private ItemStack type = ItemStack.EMPTY;
    private int count;
    private final int stacks;
    private boolean locked;
    private final LazyOptional<IItemHandler> cap = LazyOptional.of(Handler::new);

    public BarrelBE(BlockEntityType<?> t, BlockPos pos, BlockState state, int stacks) {
        super(t, pos, state);
        this.stacks = stacks;
    }

    public int capacity() {
        return type.isEmpty() ? stacks * 64 : stacks * type.getMaxStackSize();
    }

    public ItemStack getStoredType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public boolean isLocked() {
        return locked;
    }

    public void toggleLock() {
        locked = !locked;
        if (!locked && count == 0) type = ItemStack.EMPTY;
        changed();
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (type.isEmpty() || ItemStack.isSameItemSameTags(type, stack));
    }

    /** Inserts and returns the leftover. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) return stack;
        int cap = stacks * stack.getMaxStackSize();
        int put = Math.min(stack.getCount(), cap - count);
        if (put <= 0) return stack;
        if (!simulate) {
            if (type.isEmpty()) type = stack.copyWithCount(1);
            count += put;
            changed();
        }
        ItemStack rem = stack.copy();
        rem.shrink(put);
        return rem;
    }

    public ItemStack extract(int amount, boolean simulate) {
        if (type.isEmpty() || count <= 0) return ItemStack.EMPTY;
        int take = Math.min(amount, Math.min(count, type.getMaxStackSize()));
        ItemStack out = type.copyWithCount(take);
        if (!simulate) {
            count -= take;
            if (count <= 0 && !locked) type = ItemStack.EMPTY;
            changed();
        }
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeContents(tag);
    }

    public void writeContents(CompoundTag tag) {
        if (!type.isEmpty()) tag.put("Type", type.save(new CompoundTag()));
        tag.putInt("Count", count);
        tag.putBoolean("Locked", locked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        type = tag.contains("Type") ? ItemStack.of(tag.getCompound("Type")) : ItemStack.EMPTY;
        count = tag.getInt("Count");
        locked = tag.getBoolean("Locked");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> c, @Nullable Direction side) {
        if (c == ForgeCapabilities.ITEM_HANDLER) return cap.cast();
        return super.getCapability(c, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
    }

    private class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 1 || type.isEmpty()) return ItemStack.EMPTY;
            return type.copyWithCount(Math.min(count, type.getMaxStackSize()));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == 0 ? insert(stack, simulate) : stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? extract(amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? stacks * 64 : 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && accepts(stack);
        }
    }
}
