package com.frontierworks.jetpack;

import com.frontierworks.core.energy.IEnergyItem;
import com.frontierworks.core.energy.ItemEnergy;
import com.frontierworks.core.net.CoreNet;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

/** Energy-powered jetpack worn in the chest slot. Hold jump to fly; toggle hover with the hover key. */
public class JetpackItem extends ArmorItem implements IEnergyItem {
    public final int capacity;
    public final int cost;
    public final double accel;
    public final double maxUp;
    public final double boost;

    public JetpackItem(ArmorMaterial mat, Properties props, int capacity, int cost, double accel, double maxUp, double boost) {
        super(mat, Type.CHESTPLATE, props.stacksTo(1));
        this.capacity = capacity;
        this.cost = cost;
        this.accel = accel;
        this.maxUp = maxUp;
        this.boost = boost;
    }

    public static void init() {
        CoreNet.register("fw_jetpack:thrust", (player, data) -> {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof JetpackItem) {
                JetpackItem jp = (JetpackItem) chest.getItem();
                ItemEnergy.use(chest, Math.min(ItemEnergy.get(chest), data == 2 ? jp.cost / 3 : jp.cost));
                player.fallDistance = 0;
            }
        });
        CoreNet.register("fw_jetpack:hover", (player, data) -> {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof JetpackItem) {
                CompoundTag t = chest.getOrCreateTag();
                t.putBoolean("Hover", !t.getBoolean("Hover"));
                player.displayClientMessage(Component.translatable(t.getBoolean("Hover") ? "msg.fw_jetpack.hover_on" : "msg.fw_jetpack.hover_off"), true);
            }
        });
    }

    public static boolean hover(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("Hover");
    }

    @Override
    public int getEnergyCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public int getEnergyTransfer(ItemStack stack) {
        return Math.max(1000, capacity / 100);
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
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemEnergy.tooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.fw_jetpack.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(hover(stack) ? "tooltip.fw_jetpack.hover_on" : "tooltip.fw_jetpack.hover_off").withStyle(ChatFormatting.AQUA));
    }
}
