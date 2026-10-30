package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.energy.LSEnergyStorage;
import com.lodestar.datastore.craft.CraftJob;
import com.lodestar.datastore.craft.CraftPlanner;
import com.lodestar.datastore.menu.CoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The datastore core: one per network. Holds the network's energy (paid out as upkeep every tick by the grid) and its
 * crafting jobs, and shows the network's state in its screen.
 */
public class CoreBE extends DataNodeBE implements MenuProvider, LSEntityBlock.IDropContents {
    public static final int CAPACITY = 200_000, MAX_IN = 4_000;
    public final LSEnergyStorage energy = new LSEnergyStorage(CAPACITY, MAX_IN, 0, this::setChanged);
    private final LazyOptional<LSEnergyStorage> energyCap = LazyOptional.of(() -> energy);
    public final List<CraftJob> jobs = new ArrayList<>();
    private int nextJob = 1;
    /** client copies for the screen */
    public int cStatus = DataGrid.NO_POWER, cUpkeep, cDevices, cDisks, cJobs, cTypesUsed, cTypesTotal, cEnergy;
    public long cBytesUsed, cBytesTotal;

    public CoreBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int upkeep() {
        return 2;
    }

    /** Called by the grid once per tick while online. */
    void tickJobs(DataGrid g) {
        if (jobs.isEmpty()) return;
        Iterator<CraftJob> it = jobs.iterator();
        while (it.hasNext()) {
            CraftJob j = it.next();
            if (j.tick(g, level)) it.remove();
        }
        setChanged();
    }

    @Nullable
    public CraftJob job(int id) {
        for (CraftJob j : jobs) if (j.id == id) return j;
        return null;
    }

    public CraftJob start(CraftPlanner.Plan plan) {
        CraftJob j = CraftJob.of(nextJob++, plan);
        jobs.add(j);
        setChanged();
        return j;
    }

    public void cancel(int id, DataGrid g) {
        CraftJob j = job(id);
        if (j == null) return;
        j.cancel();
        j.flush(g);
        setChanged();
    }

    @Override
    protected void nodeTick() {
        super.nodeTick();
        if (level.getGameTime() % 20 != 0) return;
        DataGrid g = grid();
        if (g == null) return;
        long[] d = g.diskStats();
        cStatus = g.status();
        cUpkeep = g.upkeep();
        cDevices = g.devices.size();
        cBytesUsed = d[0];
        cBytesTotal = d[1];
        cTypesUsed = (int) d[2];
        cTypesTotal = (int) d[3];
        cDisks = (int) d[4];
        cJobs = jobs.size();
        cEnergy = energy.getEnergyStored();
        sync();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (CraftJob j : jobs) {
            for (ItemStack s : j.bufferStacks()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
        jobs.clear();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.ls_datastore.core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CoreMenu(id, inv, this);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        energy.save(tag, "Energy");
        tag.putInt("NextJob", nextJob);
        ListTag l = new ListTag();
        for (CraftJob j : jobs) l.add(j.save());
        tag.put("Jobs", l);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        energy.load(tag, "Energy");
        nextJob = Math.max(1, tag.getInt("NextJob"));
        jobs.clear();
        for (Tag t : tag.getList("Jobs", Tag.TAG_COMPOUND)) {
            CraftJob j = CraftJob.load((CompoundTag) t);
            if (j != null) jobs.add(j);
        }
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        tag.putIntArray("S", new int[]{cStatus, cUpkeep, cDevices, cDisks, cJobs, cTypesUsed, cTypesTotal, cEnergy});
        tag.putLong("BU", cBytesUsed);
        tag.putLong("BT", cBytesTotal);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        int[] s = tag.getIntArray("S");
        if (s.length >= 8) {
            cStatus = s[0];
            cUpkeep = s[1];
            cDevices = s[2];
            cDisks = s[3];
            cJobs = s[4];
            cTypesUsed = s[5];
            cTypesTotal = s[6];
            cEnergy = s[7];
        }
        cBytesUsed = tag.getLong("BU");
        cBytesTotal = tag.getLong("BT");
    }
}
