package com.lodestar.toolsmith.station;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/** Part builder, tool station and tool forge: keep their input items between uses. */
public class StationBE extends LSBlockEntity implements MenuProvider, LSEntityBlock.IDropContents {
    public enum Kind { PART_BUILDER, TOOL_STATION, TOOL_FORGE }

    public final Kind kind;
    public final ItemStackHandler items;
    public int selected;

    public StationBE(BlockEntityType<?> type, BlockPos pos, BlockState state, Kind kind) {
        super(type, pos, state);
        this.kind = kind;
        this.items = new ItemStackHandler(kind == Kind.PART_BUILDER ? 2 : 5) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    @Override
    protected boolean isTicking() {
        return false;
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.put("Items", items.serializeNBT());
        tag.putInt("Selected", selected);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        items.deserializeNBT(tag.getCompound("Items"));
        selected = tag.getInt("Selected");
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        tag.putInt("Selected", selected);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        selected = tag.getInt("Selected");
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        LSEntityBlock.IDropContents.dropHandler(level, pos, items);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return kind == Kind.PART_BUILDER ? new PartBuilderMenu(id, inv, this) : new ToolStationMenu(id, inv, this);
    }
}
