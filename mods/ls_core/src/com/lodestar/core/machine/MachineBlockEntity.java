package com.lodestar.core.machine;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.energy.LSEnergyStorage;
import com.lodestar.core.item.AugmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Base block entity for every Lodestar machine: inventory with slot roles, optional energy and fluid tanks,
 * per-side automation config with auto-eject/auto-input, redstone control, augments, GUI data sync and
 * NBT persistence. Subclasses implement {@link #serverTick()} and check {@link #canRun()}.
 */
public abstract class MachineBlockEntity extends LSBlockEntity implements MenuProvider, LSEntityBlock.IDropContents, LSEntityBlock.IComparatorOutput {
    public static final int D_ENERGY = 0, D_MAX_ENERGY = 1, D_PROGRESS = 2, D_MAX_PROGRESS = 3, D_BURN = 4, D_MAX_BURN = 5;
    public static final int D_REDSTONE = 6, D_SIDES = 7, D_RATE = 8;
    public static final int D_TANKS = 9;

    /** Menu button ids reserved by the machine framework (custom machines use 0-99). */
    public static final int BTN_REDSTONE = 100, BTN_EJECT = 101, BTN_INPUT = 102, BTN_SIDE = 110, BTN_SIDE_BACK = 116;

    protected final MachineLayout layout;
    protected final ItemStackHandler items;
    @Nullable
    protected final LSEnergyStorage energy;
    protected final FluidTank[] tanks;
    protected final int baseCapacity;
    protected final int[] baseTankCapacity;
    protected final SideConfig sides = new SideConfig();
    protected RedstoneMode redstone = RedstoneMode.IGNORE;

    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] itemCaps = new LazyOptional[7];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] fluidCaps = new LazyOptional[7];
    private LazyOptional<IEnergyStorage> energyCap;

    protected int progress;
    protected int maxProgress;
    protected int burn;
    protected int maxBurn;
    /** energy per tick being used or produced right now (for the screen) */
    protected int rate;
    protected boolean inventoryDirty = true;
    private int litCooldown;
    private int automationTimer;
    private boolean powered;

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
        this.baseTankCapacity = new int[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            final int idx = i;
            baseTankCapacity[i] = layout.tanks.get(i).capacity;
            tanks[i] = new FluidTank(layout.tanks.get(i).capacity, fs -> isFluidValid(idx, fs)) {
                @Override
                protected void onContentsChanged() {
                    MachineBlockEntity.this.inventoryDirty = true;
                    MachineBlockEntity.this.setChanged();
                }
            };
        }
        buildCaps();
    }

    private void buildCaps() {
        for (int i = 0; i < 7; i++) {
            final Direction side = i == 6 ? null : Direction.from3DDataValue(i);
            itemCaps[i] = LazyOptional.of(() -> new AutomationItemHandler(this, side));
            fluidCaps[i] = tanks.length > 0 ? LazyOptional.of(() -> new MachineFluidHandler(this, side)) : LazyOptional.empty();
        }
        energyCap = energy != null ? LazyOptional.of(this::getEnergyForCap) : LazyOptional.empty();
    }

    private void invalidateSideCaps() {
        for (int i = 0; i < 7; i++) {
            if (itemCaps[i] != null) itemCaps[i].invalidate();
            if (fluidCaps[i] != null) fluidCaps[i].invalidate();
        }
        if (energyCap != null) energyCap.invalidate();
        buildCaps();
        if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
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

    public SideConfig getSides() {
        return sides;
    }

    public RedstoneMode getRedstone() {
        return redstone;
    }

    public Direction facing() {
        return LSEntityBlock.facingOf(getBlockState());
    }

    /** Whether the side may be used for automation in the given direction (null side = internal / always). */
    public boolean sideAllows(@Nullable Direction side, boolean input) {
        if (side == null) return true;
        SideConfig.Mode m = sides.mode(facing(), side);
        return input ? m.in() : m.out();
    }

    /** Whether this machine has side configuration (screens hide the panel otherwise). */
    public boolean hasSideConfig() {
        return true;
    }

    // ------------------------------------------------------------------ hooks

    public abstract void serverTick();

    public void clientTick() {}

    @Override
    public final void tickServer() {
        if (level == null) return;
        if ((level.getGameTime() + worldPosition.hashCode()) % 4 == 0) {
            powered = level.hasNeighborSignal(worldPosition);
        }
        if (++automationTimer >= 8) {
            automationTimer = 0;
            if (sides.autoEject) autoEject();
            if (sides.autoInput) autoInput();
        }
        serverTick();
    }

    @Override
    public final void tickClient() {
        clientTick();
    }

    /** Redstone control: may the machine work this tick? */
    public boolean canRun() {
        return redstone.allows(powered);
    }

    protected void onInventoryChanged(int slot) {
        inventoryDirty = true;
        if (layout.slots.get(slot).role == MachineLayout.Role.UPGRADE) {
            applyAugments();
        }
        setChanged();
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role role = layout.slots.get(slot).role;
        switch (role) {
            case OUTPUT:
                return false;
            case UPGRADE:
                return stack.getItem() instanceof AugmentItem a && acceptsAugment(a) && !hasAugmentKind(a.kind, slot);
            case BATTERY:
            case CHARGE:
                return EnergyUtil.get(stack) != null;
            default:
                return true;
        }
    }

    /** Which augment kinds this machine can use. */
    public boolean acceptsAugment(AugmentItem a) {
        return switch (a.kind) {
            case SPEED, EFFICIENCY -> energy != null || layout.flameX >= 0;
            case CAPACITY -> energy != null;
            case FLUID -> tanks.length > 0;
            case OUTPUT -> true;
            case RANGE -> false;
        };
    }

    private boolean hasAugmentKind(AugmentItem.Kind kind, int exceptSlot) {
        for (int i = 0; i < layout.slots.size(); i++) {
            if (i == exceptSlot || layout.slots.get(i).role != MachineLayout.Role.UPGRADE) continue;
            if (items.getStackInSlot(i).getItem() instanceof AugmentItem a && a.kind == kind) return true;
        }
        return false;
    }

    public int getSlotLimit(int slot) {
        return layout.slots.get(slot).role == MachineLayout.Role.UPGRADE ? 1 : 64;
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

    /** Handles menu button clicks. Framework ids (>= 100) are handled here; others go to {@link #onCustomButton}. */
    public final void onButton(int id, Player player) {
        if (id == BTN_REDSTONE) {
            redstone = redstone.next();
        } else if (id == BTN_EJECT) {
            sides.autoEject = !sides.autoEject;
        } else if (id == BTN_INPUT) {
            sides.autoInput = !sides.autoInput;
        } else if (id >= BTN_SIDE && id < BTN_SIDE + 6) {
            sides.cycle(id - BTN_SIDE, true);
            invalidateSideCaps();
        } else if (id >= BTN_SIDE_BACK && id < BTN_SIDE_BACK + 6) {
            sides.cycle(id - BTN_SIDE_BACK, false);
            invalidateSideCaps();
        } else {
            onCustomButton(id, player);
        }
        setChanged();
    }

    public void onCustomButton(int id, Player player) {}

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
        return D_TANKS + tanks.length * 3 + layout.extraData;
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
            case D_REDSTONE:
                return redstone.ordinal();
            case D_SIDES:
                return sides.pack();
            case D_RATE:
                return rate;
            default:
                break;
        }
        int t = i - D_TANKS;
        if (t < tanks.length * 3) {
            FluidTank tank = tanks[t / 3];
            return switch (t % 3) {
                case 0 -> tank.getFluidAmount();
                case 1 -> tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                default -> tank.getCapacity();
            };
        }
        return getExtra(t - tanks.length * 3);
    }

    // ------------------------------------------------------------------ automation

    protected void autoEject() {
        if (level == null) return;
        Direction facing = facing();
        for (int f = 0; f < 6; f++) {
            if (!sides.get(f).out()) continue;
            Direction dir = SideConfig.side(facing, f);
            BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
            if (other == null) continue;
            other.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(dest -> {
                for (int s = 0; s < layout.slots.size(); s++) {
                    if (layout.slots.get(s).role != MachineLayout.Role.OUTPUT) continue;
                    ItemStack st = items.getStackInSlot(s);
                    if (st.isEmpty()) continue;
                    ItemStack rem = ItemHandlerHelper.insertItemStacked(dest, st.copy(), false);
                    if (rem.getCount() != st.getCount()) items.setStackInSlot(s, rem);
                }
            });
            other.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(dest -> {
                for (int t = 0; t < tanks.length; t++) {
                    if (!layout.tanks.get(t).output || tanks[t].isEmpty()) continue;
                    int moved = dest.fill(tanks[t].getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                    if (moved > 0) tanks[t].drain(moved, IFluidHandler.FluidAction.EXECUTE);
                }
            });
        }
    }

    protected void autoInput() {
        if (level == null) return;
        Direction facing = facing();
        for (int f = 0; f < 6; f++) {
            if (!sides.get(f).in()) continue;
            Direction dir = SideConfig.side(facing, f);
            BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
            if (other == null) continue;
            other.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(src -> {
                for (int i = 0; i < src.getSlots(); i++) {
                    ItemStack st = src.extractItem(i, 64, true);
                    if (st.isEmpty()) continue;
                    ItemStack rem = st;
                    for (int s = 0; s < layout.slots.size() && !rem.isEmpty(); s++) {
                        MachineLayout.Role r = layout.slots.get(s).role;
                        if (r != MachineLayout.Role.INPUT && r != MachineLayout.Role.FUEL) continue;
                        if (!canAutomationInsert(s, rem)) continue;
                        rem = items.insertItem(s, rem, false);
                    }
                    int moved = st.getCount() - rem.getCount();
                    if (moved > 0) {
                        src.extractItem(i, moved, false);
                        return;
                    }
                }
            });
            if (tanks.length > 0) {
                other.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(src -> {
                    for (int t = 0; t < tanks.length; t++) {
                        if (!layout.tanks.get(t).input) continue;
                        FluidStack want = tanks[t].isEmpty() ? src.drain(tanks[t].getSpace(), IFluidHandler.FluidAction.SIMULATE)
                                : src.drain(new FluidStack(tanks[t].getFluid(), tanks[t].getSpace()), IFluidHandler.FluidAction.SIMULATE);
                        if (want.isEmpty() || !tanks[t].isFluidValid(want)) continue;
                        int filled = tanks[t].fill(want, IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) src.drain(new FluidStack(want, filled), IFluidHandler.FluidAction.EXECUTE);
                    }
                });
            }
        }
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

    /** Sum of the effect values of installed augments of a kind (0 when none). */
    public float augment(AugmentItem.Kind kind) {
        float v = 0;
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role != MachineLayout.Role.UPGRADE) continue;
            ItemStack st = items.getStackInSlot(i);
            if (st.getItem() instanceof AugmentItem a && a.kind == kind) v += a.value();
        }
        return v;
    }

    protected void applyAugments() {
        if (energy != null) {
            energy.setCapacity(Math.round(baseCapacity * (1 + augment(AugmentItem.Kind.CAPACITY))));
            energy.setMaxReceive(Math.max(energy.getMaxReceive(), 0));
        }
        float fluid = augment(AugmentItem.Kind.FLUID);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].setCapacity(Math.round(baseTankCapacity[i] * (1 + fluid)));
        }
    }

    /** Multiplier on processing time (lower is faster). */
    public double speedMultiplier() {
        return 1.0 / (1.0 + augment(AugmentItem.Kind.SPEED));
    }

    /** Multiplier on energy per tick. */
    public double energyMultiplier() {
        return (1.0 + augment(AugmentItem.Kind.SPEED) * 1.2) * (1.0 - augment(AugmentItem.Kind.EFFICIENCY));
    }

    /** Bonus applied to secondary output chances and generator output. */
    public float outputBonus() {
        return augment(AugmentItem.Kind.OUTPUT);
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

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack st = items.getStackInSlot(i);
            if (!st.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st.copy());
            }
        }
    }

    @Override
    public int comparatorOutput() {
        if (energy != null && energy.getMaxEnergyStored() > 0) {
            return Math.round(15f * energy.getEnergyStored() / energy.getMaxEnergyStored());
        }
        if (tanks.length > 0 && tanks[0].getCapacity() > 0) {
            return Math.round(15f * tanks[0].getFluidAmount() / tanks[0].getCapacity());
        }
        return 0;
    }

    public void syncToClient() {
        sync();
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
        int idx = side == null ? 6 : side.get3DDataValue();
        if (side != null && sides.mode(facing(), side) == SideConfig.Mode.NONE && cap != ForgeCapabilities.ENERGY) {
            return super.getCapability(cap, side);
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCaps[idx].cast();
        if (cap == ForgeCapabilities.ENERGY && energy != null) return energyCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && tanks.length > 0) return fluidCaps[idx].cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < 7; i++) {
            itemCaps[i].invalidate();
            fluidCaps[i].invalidate();
        }
        energyCap.invalidate();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.put("Items", items.serializeNBT());
        if (energy != null) energy.save(tag, "Energy");
        for (int i = 0; i < tanks.length; i++) {
            tag.put("Tank" + i, tanks[i].writeToNBT(new CompoundTag()));
        }
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Burn", burn);
        tag.putInt("MaxBurn", maxBurn);
        tag.putInt("Redstone", redstone.ordinal());
        sides.save(tag);
        saveExtra(tag);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        if (tag.contains("Items")) {
            CompoundTag inv = tag.getCompound("Items");
            // keep our slot count even if an older save had fewer slots
            inv.putInt("Size", items.getSlots());
            items.deserializeNBT(inv);
        }
        applyAugments();
        if (energy != null) energy.load(tag, "Energy");
        for (int i = 0; i < tanks.length; i++) {
            if (tag.contains("Tank" + i)) tanks[i].readFromNBT(tag.getCompound("Tank" + i));
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        burn = tag.getInt("Burn");
        maxBurn = tag.getInt("MaxBurn");
        redstone = RedstoneMode.values()[Math.min(2, tag.getInt("Redstone"))];
        sides.load(tag);
        loadExtra(tag);
        inventoryDirty = true;
    }

    /** Client sync: machines render from block state; only tanks are sent (for renderers showing fluids). */
    @Override
    protected void saveSync(CompoundTag tag) {
        for (int i = 0; i < tanks.length; i++) {
            tag.put("Tank" + i, tanks[i].writeToNBT(new CompoundTag()));
        }
        saveSyncExtra(tag);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        for (int i = 0; i < tanks.length; i++) {
            if (tag.contains("Tank" + i)) tanks[i].readFromNBT(tag.getCompound("Tank" + i));
        }
        loadSyncExtra(tag);
    }

    protected void saveExtra(CompoundTag tag) {}

    protected void loadExtra(CompoundTag tag) {}

    protected void saveSyncExtra(CompoundTag tag) {}

    protected void loadSyncExtra(CompoundTag tag) {}

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
