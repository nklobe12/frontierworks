package com.lodestar.ducts;

import com.lodestar.core.grid.GridNodeBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A duct segment. Member of a {@link DuctGrid} of its kind; keeps its share of the network's energy or fluid while
 * the grid is rebuilt, holds the items currently passing through it, and stores attachments and cut sides.
 */
public class DuctBE extends GridNodeBE<DuctGrid> {
    public final DuctKind kind;
    public final int tier;
    private int cut;
    private final Attachment[] attachments = new Attachment[6];
    long energyShare;
    FluidStack fluidShare = FluidStack.EMPTY;
    public final List<TravellingItem> items = new ArrayList<>();
    private int servoTimer;
    private final LazyOptional<?>[] caps = new LazyOptional[7];

    public DuctBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        DuctBlock b = (DuctBlock) state.getBlock();
        this.kind = b.kind;
        this.tier = b.tier;
    }

    public DuctBlock.Conn conn(Direction d) {
        BlockState s = getBlockState();
        return s.getBlock() instanceof DuctBlock ? s.getValue(DuctBlock.PROPS.get(d)) : DuctBlock.Conn.NONE;
    }

    public boolean isCut(Direction d) {
        return (cut & (1 << d.get3DDataValue())) != 0;
    }

    public void toggleCut(Direction d) {
        cut ^= 1 << d.get3DDataValue();
        connectionsChanged();
        sync();
    }

    @Nullable
    public Attachment attachment(Direction d) {
        return attachments[d.get3DDataValue()];
    }

    public void attach(Direction d, Attachment.Type type) {
        attachments[d.get3DDataValue()] = new Attachment(type);
        connectionsChanged();
        sync();
    }

    public ItemStack detach(Direction d) {
        Attachment a = attachments[d.get3DDataValue()];
        attachments[d.get3DDataValue()] = null;
        connectionsChanged();
        sync();
        if (a == null) return ItemStack.EMPTY;
        return new ItemStack(DuctsSetup.attachmentItem(a.type, kind == DuctKind.FLUID));
    }

    public void connectionsChanged() {
        invalidateGrid();
        for (LazyOptional<?> c : caps) if (c != null) c.invalidate();
        java.util.Arrays.fill(caps, null);
    }

    public MenuProvider attachmentMenu(Direction side) {
        return new SimpleMenuProvider((id, inv, p) -> new FilterMenu(id, inv, this, side),
                Component.translatable("gui.ls_ducts." + attachment(side).type.name().toLowerCase(java.util.Locale.ROOT)));
    }

    // ------------------------------------------------------------------ grid

    @Override
    protected DuctGrid createGrid() {
        return switch (kind) {
            case ENERGY -> new DuctGrid.Energy(level);
            case FLUID -> new DuctGrid.Fluids(level);
            case ITEM -> new DuctGrid.Items(level);
        };
    }

    @Override
    public boolean linksTo(Direction d, GridNodeBE<?> other) {
        return other instanceof DuctBE o && o.kind == kind && conn(d) == DuctBlock.Conn.DUCT;
    }

    @Override
    protected void absorbInto(DuctGrid g) {
        g.absorb(this);
    }

    @Override
    protected void releaseFrom(DuctGrid g) {
        g.release(this);
    }

    @Override
    protected void nodeTick() {
        if (kind == DuctKind.ITEM) {
            tickItems();
            if (++servoTimer >= DuctTiers.idx(DuctTiers.ITEM_INTERVAL, tier)) {
                servoTimer = 0;
                DuctGrid g = grid();
                if (g instanceof DuctGrid.Items ig) {
                    for (Direction d : Direction.values()) {
                        Attachment a = attachment(d);
                        if (a == null || conn(d) != DuctBlock.Conn.END || level.hasNeighborSignal(worldPosition)) continue;
                        if (a.type == Attachment.Type.SERVO) ig.servoPull(this, d, a);
                        else if (a.type == Attachment.Type.RETRIEVER && level.getGameTime() % 40 < DuctTiers.idx(DuctTiers.ITEM_INTERVAL, tier)) ig.retrieve(this, d, a);
                    }
                }
            }
        }
    }

    @Override
    public void tickClient() {
        for (TravellingItem ti : items) ti.progress = Math.min(1.2f, ti.progress + ti.speed);
    }

    // ------------------------------------------------------------------ travelling items

    public void addItem(TravellingItem ti) {
        ti.progress = 0;
        ti.dir = ti.path.isEmpty() ? ti.destFace : directionTo(ti.path.get(0));
        items.add(ti);
        sync();
    }

    private Direction directionTo(BlockPos next) {
        BlockPos d = next.subtract(worldPosition);
        Direction dir = Direction.fromDelta(d.getX(), d.getY(), d.getZ());
        return dir == null ? Direction.UP : dir;
    }

    private void tickItems() {
        if (items.isEmpty()) return;
        boolean changed = false;
        Iterator<TravellingItem> it = items.iterator();
        List<TravellingItem> rerouted = new ArrayList<>();
        while (it.hasNext()) {
            TravellingItem ti = it.next();
            ti.progress += ti.speed;
            if (ti.progress < 1f) continue;
            it.remove();
            changed = true;
            if (ti.path.isEmpty()) {
                ItemStack rem = deliver(ti);
                if (!rem.isEmpty()) {
                    ti.stack = rem;
                    if (!(grid() instanceof DuctGrid.Items ig) || !ig.reroute(this, ti)) dropStack(rem);
                    else rerouted.add(ti);
                }
            } else {
                BlockPos next = ti.path.remove(0);
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof DuctBE nd && nd.kind == DuctKind.ITEM) nd.addItem(ti);
                else dropStack(ti.stack);
            }
        }
        for (TravellingItem ti : rerouted) addItem(ti);
        if (changed) sync();
    }

    private ItemStack deliver(TravellingItem ti) {
        BlockEntity be = level.getBlockEntity(worldPosition.relative(ti.destFace));
        if (be == null) return ti.stack;
        IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, ti.destFace.getOpposite()).resolve().orElse(null);
        if (h == null) return ti.stack;
        return ItemHandlerHelper.insertItemStacked(h, ti.stack, false);
    }

    private void dropStack(ItemStack st) {
        if (!st.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, st);
    }

    public void dropAll() {
        if (level == null || level.isClientSide) return;
        for (TravellingItem ti : items) dropStack(ti.stack);
        items.clear();
        for (Direction d : Direction.values()) {
            if (attachment(d) != null) dropStack(new ItemStack(DuctsSetup.attachmentItem(attachment(d).type, kind == DuctKind.FLUID)));
        }
    }

    // ------------------------------------------------------------------ capabilities

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (side != null && isCut(side)) return super.getCapability(cap, side);
        if (cap != kind.capability()) return super.getCapability(cap, side);
        int i = side == null ? 6 : side.get3DDataValue();
        if (caps[i] == null) {
            caps[i] = switch (kind) {
                case ENERGY -> LazyOptional.of(EnergyIn::new);
                case FLUID -> LazyOptional.of(FluidIn::new);
                case ITEM -> LazyOptional.of(() -> new ItemIn(side));
            };
        }
        return caps[i].cast();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<?> c : caps) if (c != null) c.invalidate();
    }

    private class EnergyIn implements IEnergyStorage {
        public int receiveEnergy(int max, boolean sim) {
            return grid() instanceof DuctGrid.Energy g ? g.receive(Math.min(max, DuctTiers.idx(DuctTiers.ENERGY_RATE, tier)), sim) : 0;
        }

        public int extractEnergy(int max, boolean sim) {
            return 0;
        }

        public int getEnergyStored() {
            return grid() instanceof DuctGrid.Energy g ? (int) Math.min(Integer.MAX_VALUE, g.buffer) : 0;
        }

        public int getMaxEnergyStored() {
            return grid() instanceof DuctGrid.Energy g ? (int) Math.min(Integer.MAX_VALUE, g.capacity) : 0;
        }

        public boolean canExtract() {
            return false;
        }

        public boolean canReceive() {
            return true;
        }
    }

    private class FluidIn implements IFluidHandler {
        public int getTanks() {
            return 1;
        }

        @Nonnull
        public FluidStack getFluidInTank(int t) {
            return grid() instanceof DuctGrid.Fluids g ? g.buffer : FluidStack.EMPTY;
        }

        public int getTankCapacity(int t) {
            return grid() instanceof DuctGrid.Fluids g ? g.capacity : 0;
        }

        public boolean isFluidValid(int t, @Nonnull FluidStack s) {
            return true;
        }

        public int fill(FluidStack res, FluidAction action) {
            if (!(grid() instanceof DuctGrid.Fluids g)) return 0;
            return g.fill(new FluidStack(res, Math.min(res.getAmount(), DuctTiers.idx(DuctTiers.FLUID_RATE, tier))), action.execute());
        }

        @Nonnull
        public FluidStack drain(FluidStack res, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        public FluidStack drain(int max, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /** Lets hoppers and machines push items into the network at this duct. */
    private class ItemIn implements IItemHandler {
        @Nullable
        private final Direction side;

        ItemIn(@Nullable Direction side) {
            this.side = side;
        }

        public int getSlots() {
            return 1;
        }

        @Nonnull
        public ItemStack getStackInSlot(int s) {
            return ItemStack.EMPTY;
        }

        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean sim) {
            if (!(grid() instanceof DuctGrid.Items g) || side == null) return stack;
            return g.inject(DuctBE.this, side, stack, sim);
        }

        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean sim) {
            return ItemStack.EMPTY;
        }

        public int getSlotLimit(int slot) {
            return 64;
        }

        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(1);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.putInt("Cut", cut);
        ListTag at = new ListTag();
        for (int i = 0; i < 6; i++) {
            if (attachments[i] == null) continue;
            CompoundTag a = attachments[i].save();
            a.putInt("Side", i);
            at.add(a);
        }
        tag.put("Attach", at);
        if (grid != null && grid.isValid()) grid.shareFor(this);
        tag.putLong("Energy", energyShare);
        if (!fluidShare.isEmpty()) tag.put("Fluid", fluidShare.writeToNBT(new CompoundTag()));
        ListTag l = new ListTag();
        for (TravellingItem ti : items) l.add(ti.save(true));
        tag.put("Items", l);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        cut = tag.getInt("Cut");
        java.util.Arrays.fill(attachments, null);
        for (Tag t : tag.getList("Attach", Tag.TAG_COMPOUND)) {
            CompoundTag a = (CompoundTag) t;
            attachments[a.getInt("Side")] = Attachment.load(a);
        }
        energyShare = tag.getLong("Energy");
        fluidShare = tag.contains("Fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid")) : FluidStack.EMPTY;
        items.clear();
        for (Tag t : tag.getList("Items", Tag.TAG_COMPOUND)) items.add(TravellingItem.load((CompoundTag) t));
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        tag.putInt("Cut", cut);
        int mask = 0;
        for (int i = 0; i < 6; i++) {
            if (attachments[i] != null) mask |= (attachments[i].type.ordinal() + 1) << (i * 2);
        }
        tag.putInt("AttachMask", mask);
        ListTag l = new ListTag();
        for (TravellingItem ti : items) l.add(ti.save(false));
        tag.put("Items", l);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        cut = tag.getInt("Cut");
        int mask = tag.getInt("AttachMask");
        for (int i = 0; i < 6; i++) {
            int v = (mask >> (i * 2)) & 3;
            attachments[i] = v == 0 ? null : (attachments[i] != null && attachments[i].type.ordinal() == v - 1 ? attachments[i] : new Attachment(Attachment.Type.values()[v - 1]));
        }
        items.clear();
        for (Tag t : tag.getList("Items", Tag.TAG_COMPOUND)) items.add(TravellingItem.load((CompoundTag) t));
    }
}
