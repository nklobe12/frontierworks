package com.frontierworks.pipes;

import com.frontierworks.core.block.PipeBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemPipeBE extends BlockEntity implements PipeBlockBase.Ticking {
    private ItemStack filter = ItemStack.EMPTY;
    private int timer;

    public ItemPipeBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    public ItemStack getFilter() {
        return filter;
    }

    private static class Dest {
        final BlockPos pos;
        final Direction side;

        Dest(BlockPos pos, Direction side) {
            this.pos = pos;
            this.side = side;
        }
    }

    @Override
    public void serverTick() {
        if (level == null || !(getBlockState().getBlock() instanceof ItemPipeBlock)) return;
        ItemPipeBlock block = (ItemPipeBlock) getBlockState().getBlock();
        if (!block.extracts() || ++timer < block.interval) return;
        timer = 0;
        if (level.hasNeighborSignal(worldPosition)) return; // redstone pauses extraction
        for (Direction d : Direction.values()) {
            BlockPos src = worldPosition.relative(d);
            BlockEntity be = level.getBlockEntity(src);
            if (be == null || be instanceof ItemPipeBE) continue;
            IItemHandler from = be.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).orElse(null);
            if (from == null) continue;
            List<Dest> dests = destinations(src);
            if (dests.isEmpty()) continue;
            for (int slot = 0; slot < from.getSlots(); slot++) {
                ItemStack sim = from.extractItem(slot, block.rate, true);
                if (sim.isEmpty()) continue;
                if (!filter.isEmpty() && !ItemStack.isSameItemSameTags(sim, filter)) continue;
                int moved = route(sim, dests, true);
                if (moved <= 0) continue;
                ItemStack real = from.extractItem(slot, moved, false);
                int done = route(real, dests, false);
                if (done < real.getCount()) {
                    // put back anything that unexpectedly didn't fit
                    ItemStack back = real.copyWithCount(real.getCount() - done);
                    ItemHandlerHelper.insertItem(from, back, false);
                }
                return;
            }
        }
    }

    private int route(ItemStack stack, List<Dest> dests, boolean simulate) {
        ItemStack rem = stack.copy();
        for (Dest dest : dests) {
            BlockEntity be = level.getBlockEntity(dest.pos);
            if (be == null) continue;
            IItemHandler to = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dest.side).orElse(null);
            if (to == null) continue;
            rem = ItemHandlerHelper.insertItem(to, rem, simulate);
            if (rem.isEmpty()) break;
        }
        return stack.getCount() - rem.getCount();
    }

    /** Inventories reachable through the pipe network, nearest first, excluding the source. */
    private List<Dest> destinations(BlockPos source) {
        List<Dest> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> invSeen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        while (!queue.isEmpty() && seen.size() < 2048) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos np = p.relative(d);
                if (!level.isLoaded(np)) continue;
                BlockEntity be = level.getBlockEntity(np);
                if (be instanceof ItemPipeBE) {
                    if (seen.add(np)) queue.add(np);
                } else if (be != null && !np.equals(source) && invSeen.add(np)
                        && be.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).isPresent()) {
                    out.add(new Dest(np, d.getOpposite()));
                }
            }
        }
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!filter.isEmpty()) tag.put("Filter", filter.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        filter = tag.contains("Filter") ? ItemStack.of(tag.getCompound("Filter")) : ItemStack.EMPTY;
    }
}
