package com.lodestar.backpacks;

import com.lodestar.core.block.LSBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** A backpack set down in the world: same contents as the item (by id); hoppers and ducts can reach its main slots. */
public class BackpackBE extends LSBlockEntity {
    private UUID id = UUID.randomUUID();
    private BackpackTier tier = BackpackTier.LEATHER;
    private int color = -1;
    @Nullable
    private Component name;
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    /** set when picked up by hand so breaking does not drop a second item */
    boolean pickedUp;

    public BackpackBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public UUID id() {
        return id;
    }

    public BackpackTier tier() {
        return tier;
    }

    public int color() {
        return color;
    }

    @Nullable
    public Component name() {
        return name;
    }

    public void fromItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem bi)) return;
        id = BackpackItem.id(stack);
        tier = bi.tier;
        color = bi.getColor(stack);
        name = stack.hasCustomHoverName() ? stack.getHoverName() : null;
        itemCap.invalidate();
        itemCap = LazyOptional.empty();
        sync();
    }

    public ItemStack toItem() {
        ItemStack s = new ItemStack(BackpacksSetup.backpack(tier));
        s.getOrCreateTag().putUUID(BackpackItem.TAG_ID, id);
        if (s.getItem() instanceof BackpackItem bi && color != bi.defaultColor() && color != -1) bi.setColor(s, color);
        if (name != null) s.setHoverName(name);
        return s;
    }

    @Nullable
    public Contents contents() {
        if (level == null || level.isClientSide || level.getServer() == null) return null;
        return BackpackStore.get(level.getServer()).contents(id, tier.slots());
    }

    @Override
    public void tickServer() {
        if (level == null || level.getGameTime() % 4 != 0) return;
        Contents c = contents();
        if (c != null && c.has(UpgradeItem.Kind.SMELTING)) Upgrades.smelt(level, c, 4);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && level != null && !level.isClientSide) {
            if (!itemCap.isPresent()) {
                Contents c = contents();
                if (c != null) itemCap = LazyOptional.of(() -> c.items);
            }
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.putUUID("Id", id);
        tag.putString("Tier", tier.name());
        tag.putInt("Color", color);
        if (name != null) tag.putString("Name", Component.Serializer.toJson(name));
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        if (tag.hasUUID("Id")) id = tag.getUUID("Id");
        try {
            tier = BackpackTier.valueOf(tag.getString("Tier"));
        } catch (IllegalArgumentException e) {
            tier = BackpackTier.LEATHER;
        }
        color = tag.contains("Color") ? tag.getInt("Color") : -1;
        name = tag.contains("Name") ? Component.Serializer.fromJson(tag.getString("Name")) : null;
    }
}
