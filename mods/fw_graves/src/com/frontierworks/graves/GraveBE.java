package com.frontierworks.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GraveBE extends BlockEntity {
    private final List<ItemStack> items = new ArrayList<>();
    private UUID owner;
    private String ownerName = "";

    public GraveBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void store(Player player, List<ItemStack> stacks) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        items.clear();
        items.addAll(stacks);
        setChanged();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isOwner(Player p) {
        return owner == null || owner.equals(p.getUUID());
    }

    /** Gives everything back (into inventory, dropping what doesn't fit). */
    public void restore(Player p) {
        for (ItemStack st : items) {
            if (!p.getInventory().add(st)) p.drop(st, false);
        }
        items.clear();
        setChanged();
    }

    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (ItemStack st : items) list.add(st.save(new CompoundTag()));
        tag.put("Items", list);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) items.add(ItemStack.of(list.getCompound(i)));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }
}
