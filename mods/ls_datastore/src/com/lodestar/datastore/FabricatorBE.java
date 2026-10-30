package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.item.AugmentItem;
import com.lodestar.datastore.craft.CraftJob;
import com.lodestar.datastore.craft.Pattern;
import com.lodestar.datastore.menu.DeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Runs recipe cards for the network's crafting jobs, one craft at a time. Items are taken from the job when the craft
 * starts; the results go back to the job when it finishes (or into storage if the job is gone).
 */
public class FabricatorBE extends DataNodeBE implements MenuProvider, LSEntityBlock.IDropContents {
    public static final int BASE_TIME = 16;
    public final ItemStackHandler cards = new ItemStackHandler(9) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof RecipeCardItem && !Pattern.outputOf(stack).isEmpty();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            patterns = null;
            DataGrid g = currentGrid();
            if (g != null) g.markPatternsDirty();
        }
    };
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
    @Nullable
    private List<Pattern> patterns;
    private int jobId;
    public int ticksLeft, ticksTotal;
    private final List<ItemStack> pending = new ArrayList<>();
    /** what is being made, for the screen */
    public ItemStack making = ItemStack.EMPTY;

    public FabricatorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public List<Pattern> patterns() {
        if (patterns == null) {
            List<Pattern> l = new ArrayList<>();
            if (level != null) {
                for (int i = 0; i < cards.getSlots(); i++) {
                    Pattern p = Pattern.of(cards.getStackInSlot(i), level);
                    if (p != null) l.add(p);
                }
            }
            patterns = l;
        }
        return patterns;
    }

    public boolean holds(ItemKey cardKey) {
        for (Pattern p : patterns()) if (p.cardKey.equals(cardKey)) return true;
        return false;
    }

    public boolean idle() {
        return ticksLeft <= 0 && pending.isEmpty();
    }

    public boolean workingFor(int job) {
        return !idle() && jobId == job;
    }

    public int craftTime() {
        float v = 0;
        for (int i = 0; i < augments.getSlots(); i++) if (augments.getStackInSlot(i).getItem() instanceof AugmentItem a) v += a.value();
        return Math.max(2, Math.round(BASE_TIME / (1 + v * 2)));
    }

    /** Starts one craft with these items (already taken from the job). False if they do not make the card's output. */
    public boolean start(int job, Pattern p, ItemStack[] items) {
        Pattern.Result r = p.craft(level, items);
        if (r == null) return false;
        jobId = job;
        pending.clear();
        pending.add(r.output());
        pending.addAll(r.remainders());
        making = r.output().copy();
        ticksTotal = ticksLeft = craftTime();
        sync();
        return true;
    }

    @Override
    public int upkeep() {
        return ticksLeft > 0 ? 12 : 2;
    }

    @Override
    protected boolean litNow() {
        return super.litNow() && !idle();
    }

    @Override
    protected void nodeTick() {
        super.nodeTick();
        if (ticksLeft > 0) {
            if (network() == null) return;
            ticksLeft--;
        }
        if (ticksLeft <= 0 && !pending.isEmpty()) deliver();
    }

    private void deliver() {
        DataGrid g = network();
        if (g == null) return;
        CraftJob j = g.core == null ? null : g.core.job(jobId);
        if (j != null) {
            j.receive(pending);
            pending.clear();
        } else {
            pending.removeIf(s -> g.insert(s, false).isEmpty());
        }
        if (pending.isEmpty()) {
            making = ItemStack.EMPTY;
            ticksTotal = 0;
            sync();
        }
        setChanged();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        LSEntityBlock.IDropContents.dropHandler(level, pos, cards);
        LSEntityBlock.IDropContents.dropHandler(level, pos, augments);
        for (ItemStack s : pending) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        pending.clear();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return DeviceMenu.fabricator(id, inv, this);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.put("Cards", cards.serializeNBT());
        tag.put("Augments", augments.serializeNBT());
        tag.putInt("Job", jobId);
        tag.putInt("Left", ticksLeft);
        tag.putInt("Total", ticksTotal);
        ListTag p = new ListTag();
        for (ItemStack s : pending) p.add(s.save(new CompoundTag()));
        tag.put("Pending", p);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        if (tag.contains("Cards")) cards.deserializeNBT(tag.getCompound("Cards"));
        if (tag.contains("Augments")) augments.deserializeNBT(tag.getCompound("Augments"));
        jobId = tag.getInt("Job");
        ticksLeft = tag.getInt("Left");
        ticksTotal = tag.getInt("Total");
        pending.clear();
        for (Tag t : tag.getList("Pending", Tag.TAG_COMPOUND)) pending.add(ItemStack.of((CompoundTag) t));
        making = pending.isEmpty() ? ItemStack.EMPTY : pending.get(0).copy();
        patterns = null;
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        tag.putInt("Left", ticksLeft);
        tag.putInt("Total", ticksTotal);
        if (!making.isEmpty()) tag.put("Making", making.save(new CompoundTag()));
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        ticksLeft = tag.getInt("Left");
        ticksTotal = tag.getInt("Total");
        making = tag.contains("Making") ? ItemStack.of(tag.getCompound("Making")) : ItemStack.EMPTY;
    }
}
