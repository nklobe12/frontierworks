package com.frontierworks.wireless;

import com.frontierworks.core.energy.EnergyUtil;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Wireless energy node. Transmitters accept energy from cables; receivers linked to a transmitter
 * (with a Link Card) pull energy from it every tick - any distance, any loaded dimension.
 */
public class WirelessBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(80, 17, 52).slot(8, 35, MachineLayout.Role.UPGRADE)
            .info(26, 72).extra(1).build();
    private final boolean transmitter;
    private final int rate;
    @Nullable
    private BlockPos linkPos;
    @Nullable
    private ResourceLocation linkDim;
    private boolean linked;

    public WirelessBE(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean transmitter, int rate) {
        super(type, pos, state, LAYOUT, rate * 100, transmitter ? rate : 0, transmitter ? 0 : rate);
        this.transmitter = transmitter;
        this.rate = rate;
    }

    public void link(BlockPos pos, ResourceLocation dim) {
        this.linkPos = pos;
        this.linkDim = dim;
        setChanged();
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null || transmitter) return;
        linked = false;
        if (linkPos != null && linkDim != null && level.getServer() != null) {
            ServerLevel other = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, linkDim));
            if (other != null && other.isLoaded(linkPos)) {
                BlockEntity be = other.getBlockEntity(linkPos);
                if (be instanceof WirelessBE && ((WirelessBE) be).transmitter && ((WirelessBE) be).energy != null) {
                    linked = true;
                    WirelessBE tx = (WirelessBE) be;
                    int want = Math.min(rate * (1 + countUpgrades(com.frontierworks.core.item.UpgradeItem.Type.SPEED)), energy.space());
                    int got = tx.energy.consume(Math.min(want, tx.energy.getEnergyStored()));
                    energy.produce(got);
                }
            }
        }
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, rate * 4, null);
        setLit(linked);
    }

    @Override
    public int getExtra(int i) {
        return transmitter ? 2 : (linked ? 1 : 0);
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int s = menu.extra(0);
        lines.add(Component.translatable(s == 2 ? "gui.fw_wireless.transmitter" : s == 1 ? "gui.fw_wireless.linked" : "gui.fw_wireless.unlinked"));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        if (linkPos != null && linkDim != null) {
            tag.putLong("LinkPos", linkPos.asLong());
            tag.putString("LinkDim", linkDim.toString());
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        if (tag.contains("LinkPos")) {
            linkPos = BlockPos.of(tag.getLong("LinkPos"));
            linkDim = new ResourceLocation(tag.getString("LinkDim"));
        }
    }

    public boolean isTransmitter() {
        return transmitter;
    }

    public static ResourceLocation dimOf(Level level) {
        return level.dimension().location();
    }
}
