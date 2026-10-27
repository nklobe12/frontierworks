package com.lodestar.core.machine;

import com.lodestar.core.recipe.ChanceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/** Generic electric processing machine driven by a {@link MachineSpec}. */
public class ProcessingMachineBE extends MachineBlockEntity {
    protected final MachineSpec spec;
    protected final int[] inputSlots;
    protected ProcessJob job;
    private net.minecraft.resources.ResourceLocation lastRecipe;

    public ProcessingMachineBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineSpec spec) {
        super(type, pos, state, spec.layout, spec.energyCapacity, spec.maxReceive, 0);
        this.spec = spec;
        this.inputSlots = spec.layout.indices(MachineLayout.Role.INPUT);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.INPUT) {
            return level == null || spec.recipes.accepts(level, stack);
        }
        return super.isItemValid(slot, stack);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return level == null || spec.recipes.acceptsFluid(level, stack);
    }

    protected int energyPerTick(ProcessJob j) {
        return Math.max(1, (int) Math.round(j.energyPerTick * spec.energyFactor * energyMultiplier()));
    }

    protected int jobTime(ProcessJob j) {
        return Math.max(1, (int) Math.round(j.time * spec.timeFactor * speedMultiplier()));
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        dischargeBatteries();
        if (inventoryDirty) {
            inventoryDirty = false;
            ProcessJob found = spec.recipes.find(level, this, inputSlots);
            if (found == null) {
                job = null;
                progress = 0;
                lastRecipe = null;
            } else {
                if (lastRecipe == null || !lastRecipe.equals(found.recipeId)) {
                    progress = 0;
                }
                job = found;
                lastRecipe = found.recipeId;
                maxProgress = jobTime(found);
            }
        }
        boolean active = false;
        if (job != null && canRun() && canFinish(job) && consumePower(job)) {
            progress++;
            active = true;
            if (progress >= maxProgress) {
                finish(job);
                progress = 0;
                inventoryDirty = true;
            }
            setChanged();
        }
        rate = active && job != null ? energyPerTick(job) : 0;
        idleTick(active);
        setLit(active);
    }

    /** Pays for one tick of work. Default: Forge Energy. Returns false if the machine can't run. */
    protected boolean consumePower(ProcessJob j) {
        int ept = energyPerTick(j);
        if (energy == null) return true;
        if (energy.getEnergyStored() < ept) return false;
        energy.consume(ept);
        return true;
    }

    protected void idleTick(boolean active) {}

    protected boolean canFinish(ProcessJob j) {
        List<ItemStack> outs = new ArrayList<>();
        for (ChanceStack cs : j.outputs) outs.add(cs.stack);
        if (!outputsFit(outs)) return false;
        int t1 = -1;
        if (!j.fluidOut.isEmpty()) {
            t1 = outputTank(j.fluidOut, -1);
            if (t1 < 0) return false;
        }
        if (!j.fluidOut2.isEmpty() && outputTank(j.fluidOut2, t1) < 0) return false;
        return true;
    }

    /** Output tank that can take all of fs (excluding skip), or -1. */
    protected int outputTank(FluidStack fs, int skip) {
        for (int t = 0; t < tanks.length; t++) {
            if (t == skip || !layout.tanks.get(t).output) continue;
            if (tanks[t].fill(fs, IFluidHandler.FluidAction.SIMULATE) >= fs.getAmount()) return t;
        }
        return -1;
    }

    protected void finish(ProcessJob j) {
        for (int i = 0; i < j.slots.length; i++) {
            items.extractItem(j.slots[i], j.counts[i], false);
        }
        int used = -1;
        if (!j.fluidIn.isEmpty()) {
            used = RecipeSource.inputTank(this, j.fluidIn, -1);
            if (used >= 0) tanks[used].drain(j.fluidIn.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (!j.fluidIn2.isEmpty()) {
            int t = RecipeSource.inputTank(this, j.fluidIn2, used);
            if (t >= 0) tanks[t].drain(j.fluidIn2.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        for (ChanceStack cs : j.outputs) {
            ItemStack out = cs.roll(level.random, outputBonus());
            if (!out.isEmpty()) insertOutput(out, false);
        }
        int filled = -1;
        if (!j.fluidOut.isEmpty()) {
            filled = outputTank(j.fluidOut, -1);
            if (filled >= 0) tanks[filled].fill(j.fluidOut.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (!j.fluidOut2.isEmpty()) {
            int t = outputTank(j.fluidOut2, filled);
            if (t >= 0) tanks[t].fill(j.fluidOut2.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        onCrafted(j);
    }

    protected void onCrafted(ProcessJob j) {}

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int max = menu.data(D_MAX_PROGRESS);
        int p = menu.data(D_PROGRESS);
        if (max > 0 && p > 0) {
            lines.add(Component.literal((p * 100 / max) + "%"));
        }
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        if (lastRecipe != null) tag.putString("LastRecipe", lastRecipe.toString());
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        if (tag.contains("LastRecipe")) lastRecipe = new net.minecraft.resources.ResourceLocation(tag.getString("LastRecipe"));
    }

    protected static boolean isEmpty(FluidStack fs) {
        return fs == null || fs.isEmpty();
    }
}
