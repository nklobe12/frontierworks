package com.lodestar.toolsmith.foundry;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.multiblock.IMultiblockController;
import com.lodestar.core.multiblock.IMultiblockPart;
import com.lodestar.toolsmith.ToolsmithSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Foundry controller. Sits in the wall of a room of scorched bricks of any size up to 9x9x10 and melts what is put
 * inside (through its screen, hoppers, or by throwing items in) into a pool of molten metal, burning lava from the
 * foundry tanks in its walls. Molten metals that make an alloy mix automatically. Drains in the walls let faucets
 * pour the bottom fluid out; click a fluid in the screen to move it to the bottom.
 */
public class FoundryControllerBE extends LSBlockEntity implements MenuProvider, IMultiblockController, LSEntityBlock.IDropContents {
    public static final int SLOTS = 36;
    private static final int MAX_SIDE = 9, MAX_HEIGHT = 10;

    public final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot < usableSlots && !FoundryRules.melting(stack).isEmpty();
        }

        @Override
        protected void onContentsChanged(int slot) {
            progress[slot] = 0;
            setChanged();
            dirtySync = true;
        }
    };
    public final List<FluidStack> fluids = new ArrayList<>();
    public final int[] progress = new int[SLOTS];
    public final int[] needed = new int[SLOTS];
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(PoolHandler::new);
    private final LazyOptional<net.minecraftforge.items.IItemHandler> itemCap = LazyOptional.of(() -> items);

    public boolean formed;
    public int capacity;
    public int usableSlots;
    public BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
    private final List<BlockPos> tanks = new ArrayList<>();
    private final List<BlockPos> parts = new ArrayList<>();
    private int checkTimer;
    private int fuelTimer;
    private boolean dirtySync;
    public boolean heating;

    public FoundryControllerBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int stored() {
        int n = 0;
        for (FluidStack f : fluids) n += f.getAmount();
        return n;
    }

    public void requestCheck() {
        checkTimer = 0;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    // ------------------------------------------------------------------ structure

    private static boolean isWall(BlockState s) {
        return s.is(ToolsmithSetup.FOUNDRY_WALLS);
    }

    private static boolean isFloor(BlockState s) {
        return s.is(ToolsmithSetup.FOUNDRY_FLOOR);
    }

    private void detect() {
        Direction facing = LSEntityBlock.facingOf(getBlockState());
        Direction back = facing.getOpposite(), left = facing.getClockWise(), right = facing.getCounterClockWise();
        BlockPos start = worldPosition.relative(back);
        boolean ok = level.getBlockState(start).isAir();
        int l = 0, r = 0, d = 0;
        if (ok) {
            while (l < MAX_SIDE - 1 && level.getBlockState(start.relative(left, l + 1)).isAir()) l++;
            while (r < MAX_SIDE - 1 - l && level.getBlockState(start.relative(right, r + 1)).isAir()) r++;
            while (d < MAX_SIDE - 1 && level.getBlockState(start.relative(back, d + 1)).isAir()) d++;
        }
        List<BlockPos> newParts = new ArrayList<>();
        List<BlockPos> newTanks = new ArrayList<>();
        int height = 0;
        BlockPos c1 = start.relative(left, l), c2 = start.relative(right, r).relative(back, d);
        int x0 = Math.min(c1.getX(), c2.getX()), x1 = Math.max(c1.getX(), c2.getX());
        int z0 = Math.min(c1.getZ(), c2.getZ()), z1 = Math.max(c1.getZ(), c2.getZ());
        if (ok) {
            // floor
            for (int x = x0; x <= x1 && ok; x++)
                for (int z = z0; z <= z1 && ok; z++)
                    if (!isFloor(level.getBlockState(new BlockPos(x, worldPosition.getY() - 1, z)))) ok = false;
        }
        if (ok) {
            for (int y = worldPosition.getY(); y < worldPosition.getY() + MAX_HEIGHT; y++) {
                if (!layerValid(x0, x1, z0, z1, y, newParts, newTanks, y == worldPosition.getY())) break;
                height++;
            }
            ok = height > 0;
        }
        boolean was = formed;
        formed = ok;
        if (formed) {
            min = new BlockPos(x0, worldPosition.getY(), z0);
            max = new BlockPos(x1, worldPosition.getY() + height - 1, z1);
            int volume = (x1 - x0 + 1) * (z1 - z0 + 1) * height;
            capacity = volume * FoundryRules.BLOCK;
            usableSlots = Math.min(SLOTS, volume);
        } else {
            capacity = 0;
            usableSlots = 0;
        }
        for (BlockPos p : parts) if (!newParts.contains(p)) setPart(p, null);
        for (BlockPos p : newParts) setPart(p, worldPosition);
        parts.clear();
        parts.addAll(newParts);
        tanks.clear();
        tanks.addAll(newTanks);
        if (was != formed) dirtySync = true;
    }

    private boolean layerValid(int x0, int x1, int z0, int z1, int y, List<BlockPos> partsOut, List<BlockPos> tanksOut, boolean first) {
        List<BlockPos> lp = new ArrayList<>(), lt = new ArrayList<>();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return false;
        for (int x = x0; x <= x1; x++) {
            if (!wall(new BlockPos(x, y, z0 - 1), lp, lt) || !wall(new BlockPos(x, y, z1 + 1), lp, lt)) return false;
        }
        for (int z = z0; z <= z1; z++) {
            if (!wall(new BlockPos(x0 - 1, y, z), lp, lt) || !wall(new BlockPos(x1 + 1, y, z), lp, lt)) return false;
        }
        partsOut.addAll(lp);
        tanksOut.addAll(lt);
        return true;
    }

    private boolean wall(BlockPos p, List<BlockPos> lp, List<BlockPos> lt) {
        if (p.equals(worldPosition)) return true;
        BlockState s = level.getBlockState(p);
        if (!isWall(s)) return false;
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof IMultiblockPart) lp.add(p);
        if (be instanceof FoundryTankBE) lt.add(p);
        return true;
    }

    private void setPart(BlockPos p, @Nullable BlockPos ctrl) {
        if (level.getBlockEntity(p) instanceof IMultiblockPart part) part.setController(ctrl);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tickServer() {
        if (--checkTimer <= 0) {
            checkTimer = 40;
            detect();
        }
        heating = false;
        if (formed) {
            if (level.getGameTime() % 10 == 0) absorbItems();
            melt();
            alloy();
        }
        BlockState st = getBlockState();
        if (st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT) != heating) {
            level.setBlock(worldPosition, st.setValue(BlockStateProperties.LIT, heating), 3);
        }
        if (dirtySync && level.getGameTime() % 5 == 0) {
            dirtySync = false;
            sync();
        }
    }

    private boolean hasFuel() {
        if (fuelTimer > 0) return true;
        for (BlockPos p : tanks) {
            if (level.getBlockEntity(p) instanceof FoundryTankBE t && t.tank.getFluidAmount() >= 10 && FoundryTankBE.isFuel(t.tank.getFluid())) {
                t.tank.drain(10, IFluidHandler.FluidAction.EXECUTE);
                fuelTimer = 40;
                return true;
            }
        }
        return false;
    }

    private void melt() {
        boolean any = false;
        for (int i = 0; i < usableSlots; i++) {
            ItemStack st = items.getStackInSlot(i);
            if (st.isEmpty()) continue;
            FluidStack res = FoundryRules.melting(st);
            if (res.isEmpty()) continue;
            needed[i] = 20 + res.getAmount() / 8 * FoundryRules.temperature(res) / 1000;
            if (!any) {
                if (!hasFuel()) return;
                any = true;
            }
            if (progress[i] < needed[i]) {
                progress[i]++;
            } else if (stored() + res.getAmount() <= capacity) {
                addFluid(res);
                items.setStackInSlot(i, ItemStack.EMPTY);
                progress[i] = 0;
            }
        }
        if (any) {
            heating = true;
            fuelTimer--;
            dirtySync = true;
            if (level.random.nextInt(8) == 0 && level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LAVA, min.getX() + level.random.nextDouble() * (max.getX() - min.getX() + 1), min.getY() + 0.2,
                        min.getZ() + level.random.nextDouble() * (max.getZ() - min.getZ() + 1), 1, 0, 0, 0, 0);
            }
        }
    }

    public void addFluid(FluidStack fs) {
        for (FluidStack f : fluids) {
            if (f.isFluidEqual(fs)) {
                f.grow(fs.getAmount());
                return;
            }
        }
        fluids.add(fs.copy());
    }

    private void alloy() {
        for (FoundryRules.Alloy a : FoundryRules.alloys()) {
            int times = 36;
            for (FluidStack in : a.inputs()) {
                int have = 0;
                for (FluidStack f : fluids) if (f.isFluidEqual(in)) have = f.getAmount();
                times = Math.min(times, have / in.getAmount());
            }
            if (times <= 0) continue;
            int out = a.output().getAmount() * times;
            int consumed = 0;
            for (FluidStack in : a.inputs()) consumed += in.getAmount() * times;
            if (stored() - consumed + out > capacity) continue;
            for (FluidStack in : a.inputs()) {
                for (FluidStack f : fluids) if (f.isFluidEqual(in)) f.shrink(in.getAmount() * times);
            }
            fluids.removeIf(FluidStack::isEmpty);
            addFluid(new FluidStack(a.output().getFluid(), out));
            dirtySync = true;
        }
    }

    private void absorbItems() {
        AABB box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack st = e.getItem();
            for (int i = 0; i < usableSlots && !st.isEmpty(); i++) {
                if (items.getStackInSlot(i).isEmpty() && items.isItemValid(i, st)) {
                    items.setStackInSlot(i, st.split(1));
                }
            }
            if (st.isEmpty()) e.discard();
            else e.setItem(st);
        }
    }

    /** Moves fluid i to the bottom so drains pour it first. */
    public void select(int i) {
        if (i > 0 && i < fluids.size()) {
            FluidStack f = fluids.remove(i);
            fluids.add(0, f);
            dirtySync = true;
            setChanged();
        }
    }

    // ------------------------------------------------------------------ capabilities

    private class PoolHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return Math.max(1, fluids.size());
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank < fluids.size() ? fluids.get(tank) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (fluids.isEmpty() || !fluids.get(0).isFluidEqual(resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (fluids.isEmpty()) return FluidStack.EMPTY;
            FluidStack f = fluids.get(0);
            int n = Math.min(maxDrain, f.getAmount());
            FluidStack out = new FluidStack(f, n);
            if (action.execute()) {
                f.shrink(n);
                if (f.isEmpty()) fluids.remove(0);
                dirtySync = true;
                setChanged();
            }
            return out;
        }
    }

    @Override
    public <T> LazyOptional<T> getPortCapability(Capability<T> cap, BlockPos portPos, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && formed) return fluidCap.cast();
        return LazyOptional.empty();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
        itemCap.invalidate();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) for (BlockPos p : parts) setPart(p, null);
        super.setRemoved();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        LSEntityBlock.IDropContents.dropHandler(level, pos, items);
    }

    // ------------------------------------------------------------------ persistence & sync

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.put("Items", items.serializeNBT());
        tag.putIntArray("Progress", progress);
        tag.putInt("Fuel", fuelTimer);
        saveSync(tag);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        int[] p = tag.getIntArray("Progress");
        System.arraycopy(p, 0, progress, 0, Math.min(p.length, SLOTS));
        fuelTimer = tag.getInt("Fuel");
        loadSync(tag);
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        ListTag l = new ListTag();
        for (FluidStack f : fluids) l.add(f.writeToNBT(new CompoundTag()));
        tag.put("Fluids", l);
        tag.putBoolean("Formed", formed);
        tag.putInt("Capacity", capacity);
        tag.putInt("Usable", usableSlots);
        tag.put("Min", NbtUtils.writeBlockPos(min));
        tag.put("Max", NbtUtils.writeBlockPos(max));
        tag.putIntArray("Prog", progress);
        tag.putIntArray("Need", needed);
        tag.putBoolean("Heating", heating);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        fluids.clear();
        for (Tag t : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            FluidStack f = FluidStack.loadFluidStackFromNBT((CompoundTag) t);
            if (!f.isEmpty()) fluids.add(f);
        }
        formed = tag.getBoolean("Formed");
        capacity = tag.getInt("Capacity");
        usableSlots = tag.getInt("Usable");
        min = NbtUtils.readBlockPos(tag.getCompound("Min"));
        max = NbtUtils.readBlockPos(tag.getCompound("Max"));
        int[] p = tag.getIntArray("Prog");
        System.arraycopy(p, 0, progress, 0, Math.min(p.length, SLOTS));
        int[] n = tag.getIntArray("Need");
        System.arraycopy(n, 0, needed, 0, Math.min(n.length, SLOTS));
        heating = tag.getBoolean("Heating");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return formed ? new AABB(min.getX() - 1, min.getY() - 1, min.getZ() - 1, max.getX() + 2, max.getY() + 2, max.getZ() + 2)
                : super.getRenderBoundingBox();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ls_toolsmith.foundry_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FoundryMenu(id, inv, this);
    }
}
