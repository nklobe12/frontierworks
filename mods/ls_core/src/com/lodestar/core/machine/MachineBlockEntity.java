package com.lodestar.core.machine;

import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.energy.LSEnergyStorage;
import com.lodestar.core.item.UpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Base block entity for every Lodestar machine: inventory with roles, optional energy and
 * fluid tanks, capability exposure, GUI data sync, lit-state handling and NBT persistence.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int D_ENERGY = 0, D_MAX_ENERGY = 1, D_PROGRESS = 2, D_MAX_PROGRESS = 3, D_BURN = 4, D_MAX_BURN = 5;
    public static final int D_TANKS = 6;

    protected final MachineLayout layout;
    protected final ItemStackHandler items;
    @Nullable
    protected final LSEnergyStorage energy;
    protected final FluidTank[] tanks;
    protected final int baseCapacity;

    private final LazyOptional<IItemHandler> itemCap;
    private final LazyOptional<IEnergyStorage> energyCap;
    private final LazyOptional<IFluidHandler> fluidCap;

    protected int progress;
    protected int maxProgress;
    protected int burn;
    protected int maxBurn;
    protected boolean inventoryDirty = true;
    private int litCooldown;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout, int energyCapacity, int maxReceive, int maxExtract) {
        super(type, pos, state);
        this.layout = layout;
        this.baseCapacity = energyCapacity;
        this.items = new ItemStackHandler(layout.slots.size()) {
            @Override
            protected void onContentsChanged(int slot) {
                MachineBlockEntity.this.onInventoryChanged(slot);
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return MachineBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return MachineBlockEntity.this.getSlotLimit(slot);
            }
        };
        this.energy = energyCapacity > 0 ? new LSEnergyStorage(energyCapacity, maxReceive, maxExtract, this::setChanged) : null;
        this.tanks = new FluidTank[layout.tanks.size()];
        for (int i = 0; i < tanks.length; i++) {
            final int idx = i;
            tanks[i] = new FluidTank(layout.tanks.get(i).capacity, fs -> isFluidValid(idx, fs)) {
                @Override
                protected void onContentsChanged() {
                    MachineBlockEntity.this.inventoryDirty = true;
                    MachineBlockEntity.this.setChanged();
                }
            };
        }
        this.itemCap = LazyOptional.of(() -> new AutomationItemHandler(this));
        this.energyCap = energy != null ? LazyOptional.of(this::getEnergyForCap) : LazyOptional.empty();
        this.fluidCap = tanks.length > 0 ? LazyOptional.of(() -> new MachineFluidHandler(this)) : LazyOptional.empty();
    }

    /** Energy storage exposed to cables; override to restrict (e.g. generators expose extract-only). */
    protected IEnergyStorage getEnergyForCap() {
        return energy;
    }

    public MachineLayout getLayout() {
        return layout;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    @Nullable
    public LSEnergyStorage getEnergy() {
        return energy;
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int i) {
        return tanks[i];
    }

    // ------------------------------------------------------------------ hooks

    public abstract void serverTick();

    public void clientTick() {}

    protected void onInventoryChanged(int slot) {
        inventoryDirty = true;
        if (layout.slots.get(slot).role == MachineLayout.Role.UPGRADE) {
            applyUpgrades();
        }
        setChanged();
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role role = layout.slots.get(slot).role;
        switch (role) {
            case OUTPUT:
                return false;
            case UPGRADE:
                return stack.getItem() instanceof UpgradeItem;
            case BATTERY:
            case CHARGE:
                return EnergyUtil.get(stack) != null;
            default:
                return true;
        }
    }

    public int getSlotLimit(int slot) {
        return layout.slots.get(slot).role == MachineLayout.Role.UPGRADE ? 4 : 64;
    }

    /** Extra filter for pipes/hoppers on top of isItemValid. */
    public boolean canAutomationInsert(int slot, ItemStack stack) {
        return isItemValid(slot, stack);
    }

    /** CHARGE slots can be emptied by automation once the item is full. */
    public boolean isChargeDone(int slot) {
        IEnergyStorage es = EnergyUtil.get(items.getStackInSlot(slot));
        return es == null || es.getEnergyStored() >= es.getMaxEnergyStored();
    }

    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    public void onButton(int id, Player player) {}

    /** Client side: extra lines drawn in the info area (uses synced menu data). */
    public void addInfo(List<Component> lines, MachineMenu menu) {}

    /** Client side: label for a layout button. */
    public Component buttonLabel(int id, MachineMenu menu) {
        for (MachineLayout.ButtonDef b : layout.buttons) {
            if (b.id == id) return Component.translatable(b.labelKey);
        }
        return Component.empty();
    }

    /** Extra synced integer #i (count given by layout.extraData). */
    public int getExtra(int i) {
        return 0;
    }

    // ------------------------------------------------------------------ data sync

    public int dataCount() {
        return D_TANKS + tanks.length * 2 + layout.extraData;
    }

    public int getData(int i) {
        switch (i) {
            case D_ENERGY:
                return energy == null ? 0 : energy.getEnergyStored();
            case D_MAX_ENERGY:
                return energy == null ? 0 : energy.getMaxEnergyStored();
            case D_PROGRESS:
                return progress;
            case D_MAX_PROGRESS:
                return maxProgress;
            case D_BURN:
                return burn;
            case D_MAX_BURN:
                return maxBurn;
            default:
                break;
        }
        int t = i - D_TANKS;
        if (t < tanks.length * 2) {
            FluidTank tank = tanks[t / 2];
            if (t % 2 == 0) return tank.getFluidAmount();
            return tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
        }
        return getExtra(t - tanks.length * 2);
    }

    // ------------------------------------------------------------------ helpers

    protected void setLit(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(BlockStateProperties.LIT)) return;
        if (active) {
            litCooldown = 20;
        } else if (litCooldown > 0) {
            litCooldown--;
            return;
        }
        if (state.getValue(BlockStateProperties.LIT) != active) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, active), 3);
        }
    }

    public int countUpgrades(UpgradeItem.Type type) {
        int n = 0;
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role != MachineLayout.Role.UPGRADE) continue;
            ItemStack st = items.getStackInSlot(i);
            if (st.getItem() instanceof UpgradeItem && ((UpgradeItem) st.getItem()).type == type) {
                n += st.getCount();
            }
        }
        return n;
    }

    protected void applyUpgrades() {
        if (energy != null) {
            int cap = countUpgrades(UpgradeItem.Type.CAPACITY);
            energy.setCapacity(baseCapacity * (1 + cap));
        }
    }

    public double speedMultiplier() {
        return Math.pow(0.7, countUpgrades(UpgradeItem.Type.SPEED));
    }

    public double energyMultiplier() {
        return Math.pow(1.3, countUpgrades(UpgradeItem.Type.SPEED)) * Math.pow(0.8, countUpgrades(UpgradeItem.Type.EFFICIENCY));
    }

    /** Pulls energy from battery slots into the machine. */
    protected void dischargeBatteries() {
        if (energy == null) return;
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role == MachineLayout.Role.BATTERY && !energy.isFull()) {
                EnergyUtil.dischargeItem(energy, items.getStackInSlot(i), Math.max(1000, energy.getMaxReceive()));
            }
        }
    }

    /** Charges items in CHARGE slots from the machine. */
    protected void chargeItems(int rate) {
        if (energy == null) return;
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role == MachineLayout.Role.CHARGE) {
                EnergyUtil.chargeItem(energy, items.getStackInSlot(i), rate);
            }
        }
    }

    /** Inserts a stack into OUTPUT slots. Returns leftover. */
    public ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack rem = stack.copy();
        for (int i = 0; i < layout.slots.size() && !rem.isEmpty(); i++) {
            if (layout.slots.get(i).role != MachineLayout.Role.OUTPUT) continue;
            rem = insertRaw(i, rem, simulate);
        }
        return rem;
    }

    protected ItemStack insertRaw(int slot, ItemStack stack, boolean simulate) {
        ItemStack cur = items.getStackInSlot(slot);
        int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (cur.isEmpty()) {
            int put = Math.min(limit, stack.getCount());
            if (!simulate) {
                ItemStack n = stack.copy();
                n.setCount(put);
                items.setStackInSlot(slot, n);
            }
            ItemStack r = stack.copy();
            r.shrink(put);
            return r;
        }
        if (!ItemStack.isSameItemSameTags(cur, stack)) return stack;
        int put = Math.min(limit - cur.getCount(), stack.getCount());
        if (put <= 0) return stack;
        if (!simulate) {
            ItemStack n = cur.copy();
            n.grow(put);
            items.setStackInSlot(slot, n);
        }
        ItemStack r = stack.copy();
        r.shrink(put);
        return r;
    }

    /** Checks that all stacks fit into the output slots together. */
    public boolean outputsFit(List<ItemStack> stacks) {
        ItemStack[] sim = new ItemStack[layout.slots.size()];
        for (int i = 0; i < sim.length; i++) sim[i] = items.getStackInSlot(i).copy();
        for (ItemStack st : stacks) {
            ItemStack rem = st.copy();
            for (int i = 0; i < sim.length && !rem.isEmpty(); i++) {
                if (layout.slots.get(i).role != MachineLayout.Role.OUTPUT) continue;
                int limit = Math.min(rem.getMaxStackSize(), getSlotLimit(i));
                if (sim[i].isEmpty()) {
                    int put = Math.min(limit, rem.getCount());
                    sim[i] = rem.copy();
                    sim[i].setCount(put);
                    rem.shrink(put);
                } else if (ItemStack.isSameItemSameTags(sim[i], rem)) {
                    int put = Math.min(limit - sim[i].getCount(), rem.getCount());
                    if (put > 0) {
                        sim[i].grow(put);
                        rem.shrink(put);
                    }
                }
            }
            if (!rem.isEmpty()) return false;
        }
        return true;
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack st = items.getStackInSlot(i);
            if (!st.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, st.copy());
            }
        }
    }

    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public static Fluid fluidById(int id) {
        if (id < 0) return null;
        return BuiltInRegistries.FLUID.byId(id);
    }

    // ------------------------------------------------------------------ capabilities

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (cap == ForgeCapabilities.ENERGY && energy != null) return energyCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && tanks.length > 0) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        energyCap.invalidate();
        fluidCap.invalidate();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        if (energy != null) energy.save(tag, "Energy");
        for (int i = 0; i < tanks.length; i++) {
            tag.put("Tank" + i, tanks[i].writeToNBT(new CompoundTag()));
        }
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Burn", burn);
        tag.putInt("MaxBurn", maxBurn);
        saveExtra(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            CompoundTag inv = tag.getCompound("Items");
            // keep our slot count even if an older save had fewer slots
            inv.putInt("Size", items.getSlots());
            items.deserializeNBT(inv);
        }
        applyUpgrades();
        if (energy != null) energy.load(tag, "Energy");
        for (int i = 0; i < tanks.length; i++) {
            if (tag.contains("Tank" + i)) tanks[i].readFromNBT(tag.getCompound("Tank" + i));
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        burn = tag.getInt("Burn");
        maxBurn = tag.getInt("MaxBurn");
        loadExtra(tag);
        inventoryDirty = true;
    }

    protected void saveExtra(CompoundTag tag) {}

    protected void loadExtra(CompoundTag tag) {}

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineMenu(id, inv, this);
    }
}
