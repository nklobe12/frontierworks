package com.frontierworks.core.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

/** Basic Forge-Energy item (batteries, gadgets). */
public class EnergyItem extends Item implements IEnergyItem {
    protected final int capacity;
    protected final int transfer;
    protected final boolean extractable;

    public EnergyItem(Properties props, int capacity, int transfer, boolean extractable) {
        super(props.stacksTo(1));
        this.capacity = capacity;
        this.transfer = transfer;
        this.extractable = extractable;
    }

    @Override
    public int getEnergyCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public int getEnergyTransfer(ItemStack stack) {
        return transfer;
    }

    @Override
    public boolean canExtractEnergy(ItemStack stack) {
        return extractable;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return ItemEnergy.provider(this, stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemEnergy.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemEnergy.barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemEnergy.tooltip(stack, tooltip);
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
