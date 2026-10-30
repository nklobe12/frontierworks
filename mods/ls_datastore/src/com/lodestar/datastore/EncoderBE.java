package com.lodestar.datastore;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.datastore.craft.Pattern;
import com.lodestar.datastore.menu.EncoderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
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
 * Pattern encoder: lay out a recipe in nine ghost slots, put blank cards in, press Encode. Putting an encoded card in
 * the input loads its recipe back into the grid, so it can be changed and written again.
 */
public class EncoderBE extends LSBlockEntity implements MenuProvider, LSEntityBlock.IDropContents {
    public final SimpleContainer grid = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            EncoderBE.this.setChanged();
        }
    };
    /** slot 0: blank (or old) cards in, slot 1: encoded card out */
    public final ItemStackHandler cards = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && (stack.is(Content.BLANK_RECIPE_CARD.get()) || stack.getItem() instanceof RecipeCardItem);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            ItemStack s = getStackInSlot(0);
            if (slot == 0 && s.getItem() instanceof RecipeCardItem && s.getTag() != null) loadFrom(s);
        }
    };
    public boolean substitute = true;

    public EncoderBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private void loadFrom(ItemStack card) {
        ListTag g = card.getTag().getList("Grid", Tag.TAG_COMPOUND);
        for (int i = 0; i < 9; i++) grid.setItem(i, i < g.size() ? ItemStack.of(g.getCompound(i)) : ItemStack.EMPTY);
        substitute = card.getTag().getBoolean("Sub");
    }

    public List<ItemStack> gridItems() {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < 9; i++) l.add(grid.getItem(i));
        return l;
    }

    /** Writes one card; returns false when there is no blank, no room or no recipe. */
    public boolean encode() {
        ItemStack in = cards.getStackInSlot(0);
        if (in.isEmpty() || level == null) return false;
        ItemStack card = Pattern.encode(level, gridItems(), substitute, new ItemStack(Content.RECIPE_CARD.get()));
        if (card.isEmpty()) return false;
        if (!cards.insertItem(1, card, true).isEmpty()) return false;
        cards.extractItem(0, 1, false);
        cards.insertItem(1, card, false);
        return true;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        LSEntityBlock.IDropContents.dropHandler(level, pos, cards);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EncoderMenu(id, inv, this);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        ListTag g = new ListTag();
        for (int i = 0; i < 9; i++) g.add(grid.getItem(i).save(new CompoundTag()));
        tag.put("Grid", g);
        tag.put("Cards", cards.serializeNBT());
        tag.putBoolean("Sub", substitute);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        ListTag g = tag.getList("Grid", Tag.TAG_COMPOUND);
        for (int i = 0; i < 9; i++) grid.setItem(i, i < g.size() ? ItemStack.of(g.getCompound(i)) : ItemStack.EMPTY);
        if (tag.contains("Cards")) cards.deserializeNBT(tag.getCompound("Cards"));
        substitute = !tag.contains("Sub") || tag.getBoolean("Sub");
    }

    @Override
    protected void saveSync(CompoundTag tag) {}

    @Override
    protected void loadSync(CompoundTag tag) {}

    @Override
    protected boolean isTicking() {
        return false;
    }
}
