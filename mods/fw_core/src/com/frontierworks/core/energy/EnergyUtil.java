package com.frontierworks.core.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public final class EnergyUtil {
    private EnergyUtil() {}

    @Nullable
    public static IEnergyStorage get(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        return be.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
    }

    @Nullable
    public static IEnergyStorage get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
    }

    /** Pushes energy from an internal storage into neighbouring receivers. Returns total sent. */
    public static int pushToNeighbors(Level level, BlockPos pos, FWEnergyStorage source, int maxPerSide, @Nullable Predicate<Direction> sides) {
        int total = 0;
        for (Direction dir : Direction.values()) {
            if (source.getEnergyStored() <= 0) {
                break;
            }
            if (sides != null && !sides.test(dir)) {
                continue;
            }
            IEnergyStorage target = get(level, pos.relative(dir), dir.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offer = Math.min(maxPerSide, source.getEnergyStored());
            int accepted = target.receiveEnergy(offer, false);
            if (accepted > 0) {
                source.consume(accepted);
                total += accepted;
            }
        }
        return total;
    }

    /** Moves energy from storage into an item (charging). Returns amount moved. */
    public static int chargeItem(FWEnergyStorage source, ItemStack stack, int max) {
        IEnergyStorage target = get(stack);
        if (target == null || !target.canReceive()) {
            return 0;
        }
        int offer = Math.min(max, source.getEnergyStored());
        int accepted = target.receiveEnergy(offer, false);
        source.consume(accepted);
        return accepted;
    }

    /** Pulls energy from an item (battery) into storage. Returns amount moved. */
    public static int dischargeItem(FWEnergyStorage target, ItemStack stack, int max) {
        IEnergyStorage src = get(stack);
        if (src == null || !src.canExtract()) {
            return 0;
        }
        int want = Math.min(max, target.space());
        int got = src.extractEnergy(want, false);
        target.produce(got);
        return got;
    }

    /** Charges every energy item in a player's inventory from the given budget. Returns energy used. */
    public static int chargePlayerItems(Player player, int budget, @Nullable ItemStack skip) {
        int used = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && used < budget; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.isEmpty() || st == skip) continue;
            IEnergyStorage es = get(st);
            if (es != null && es.canReceive() && es.getEnergyStored() < es.getMaxEnergyStored()) {
                used += es.receiveEnergy(budget - used, false);
            }
        }
        return used;
    }

    public static String format(long fe) {
        if (fe >= 1_000_000_000L) return String.format("%.2fG", fe / 1_000_000_000.0);
        if (fe >= 1_000_000L) return String.format("%.2fM", fe / 1_000_000.0);
        if (fe >= 10_000L) return String.format("%.1fk", fe / 1_000.0);
        return Long.toString(fe);
    }

    public static Component energyLine(int stored, int max) {
        return Component.literal(format(stored) + " / " + format(max) + " FE").withStyle(ChatFormatting.RED);
    }
}
