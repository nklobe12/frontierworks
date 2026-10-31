package com.lodestar.petroleum;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.multiblock.IMultiblockController;
import com.lodestar.core.multiblock.IMultiblockPart;
import com.lodestar.core.multiblock.MultiblockPattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Distillation column reboiler. Trays stacked straight on top of it (2 to 5) make the column; it boils crude oil in
 * 100 mB batches and each tray catches one fraction: heavy oil, diesel, kerosene, naphtha, refinery gas from the bottom
 * up. Fractions above the top tray are flared off, so a taller column wastes less. Each tray exposes its fraction.
 */
public class DistillationBE extends MachineBlockEntity implements IMultiblockController {
    public static final int MAX_TRAYS = 5, BATCH = 100, BASE_TIME = 10, BASE_FE = 30;
    /** mB of each fraction per 100 mB of crude, bottom tray first */
    public static final int[] YIELD = {30, 25, 20, 15, 10};
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 38).slot(9, 59, MachineLayout.Role.BATTERY).tank(26, 17, 16, 52, 8000, true, false).arrow(46, 34)
            .tank(72, 17, 12, 52, 4000, false, true).tank(88, 17, 12, 52, 4000, false, true).tank(104, 17, 12, 52, 4000, false, true)
            .tank(120, 17, 12, 52, 4000, false, true).tank(136, 17, 12, 52, 4000, false, true)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .info(26, 73).height(176).extra(1).build();

    @SuppressWarnings("unchecked")
    public static final Supplier<Fluid>[] FRACTIONS = new Supplier[]{
            () -> Content.HEAVY_OIL.get(), () -> Content.DIESEL.get(), () -> Content.KEROSENE.get(), () -> Content.NAPHTHA.get(),
            () -> Content.REFINERY_GAS.get()};

    private static MultiblockPattern preview;

    /** Pattern of a full five-tray column, for the Codex preview. */
    public static MultiblockPattern previewPattern() {
        if (preview == null) {
            preview = MultiblockPattern.builder("ls_petroleum:distillation")
                    .layer("C").layer("T").layer("T").layer("T").layer("T").layer("T")
                    .key('T', () -> Content.DISTILLATION_TRAY.get()).build();
        }
        return preview;
    }

    private int trays;
    private int checkTimer;
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] trayCaps = new LazyOptional[MAX_TRAYS];

    public DistillationBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 30000, 600, 0);
        for (int i = 0; i < MAX_TRAYS; i++) {
            final int t = i + 1;
            trayCaps[i] = LazyOptional.of(() -> new PumpjackBE.DrainOnly(tanks[t]));
        }
    }

    public int trays() {
        return trays;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank == 0) return stack.getFluid().isSame(Content.CRUDE_OIL.get());
        return stack.getFluid().isSame(FRACTIONS[tank - 1].get());
    }

    // ------------------------------------------------------------------ structure

    private void checkColumn() {
        int n = 0;
        while (n < MAX_TRAYS && level.getBlockState(worldPosition.above(n + 1)).is(Content.DISTILLATION_TRAY.get())) n++;
        int formedTrays = n >= 2 ? n : 0;
        // trays above a too-short or broken column show as idle
        for (int i = 0; i < MAX_TRAYS + 1; i++) {
            BlockPos p = worldPosition.above(i + 1);
            BlockState st = level.getBlockState(p);
            if (!st.is(Content.DISTILLATION_TRAY.get())) break;
            int want = i < formedTrays ? i + 1 : 0;
            if (st.getValue(TrayBlock.FRACTION) != want) level.setBlock(p, st.setValue(TrayBlock.FRACTION, want), 3);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof IMultiblockPart part) part.setController(want > 0 ? worldPosition : null);
        }
        if (formedTrays != trays) {
            trays = formedTrays;
            setChanged();
        }
    }

    @Override
    public boolean isFormed() {
        return trays >= 2;
    }

    @Override
    public <T> LazyOptional<T> getPortCapability(Capability<T> cap, BlockPos portPos, @Nullable Direction side) {
        int idx = portPos.getY() - worldPosition.getY() - 1;
        if (cap == ForgeCapabilities.FLUID_HANDLER && idx >= 0 && idx < trays) return trayCaps[idx].cast();
        return LazyOptional.empty();
    }

    // ------------------------------------------------------------------ work

    public int batchTime() {
        return Math.max(2, (int) Math.round(BASE_TIME * speedMultiplier()));
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        dischargeBatteries();
        if (--checkTimer <= 0) {
            checkTimer = 20;
            checkColumn();
        }
        boolean active = false;
        maxProgress = batchTime();
        int cost = (int) Math.max(1, Math.round(BASE_FE * energyMultiplier()));
        if (isFormed() && canRun() && tanks[0].getFluidAmount() >= BATCH && fits() && energy != null && energy.getEnergyStored() >= cost) {
            energy.consume(cost);
            active = true;
            if (++progress >= maxProgress) {
                progress = 0;
                tanks[0].drain(BATCH, IFluidHandler.FluidAction.EXECUTE);
                for (int i = 0; i < trays; i++) {
                    tanks[1 + i].fill(new FluidStack(FRACTIONS[i].get(), YIELD[i]), IFluidHandler.FluidAction.EXECUTE);
                }
                if (trays < MAX_TRAYS && level instanceof ServerLevel sl && level.random.nextInt(3) == 0) {
                    BlockPos top = worldPosition.above(trays + 1);
                    sl.sendParticles(ParticleTypes.FLAME, top.getX() + 0.5, top.getY() + 0.1, top.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.01);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, top.getX() + 0.5, top.getY() + 0.4, top.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
        rate = active ? cost : 0;
        setLit(active);
    }

    /** Every tray in use has room for its share of the next batch. */
    private boolean fits() {
        for (int i = 0; i < trays; i++) {
            if (tanks[1 + i].getSpace() < YIELD[i]) return false;
        }
        return true;
    }

    @Override
    public int getExtra(int i) {
        return trays;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int t = menu.extra(0);
        lines.add(t >= 2 ? Component.translatable("gui.ls_petroleum.trays", t) : Component.translatable("gui.ls_petroleum.need_trays").withStyle(ChatFormatting.DARK_RED));
    }

    private boolean unloading;

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && !unloading) {
            for (int i = 0; i < trays; i++) {
                BlockPos p = worldPosition.above(i + 1);
                BlockState st = level.getBlockState(p);
                if (st.is(Content.DISTILLATION_TRAY.get())) level.setBlock(p, st.setValue(TrayBlock.FRACTION, 0), 3);
                if (level.getBlockEntity(p) instanceof IMultiblockPart part) part.setController(null);
            }
        }
        super.setRemoved();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<IFluidHandler> c : trayCaps) c.invalidate();
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Trays", trays);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        trays = tag.getInt("Trays");
    }
}
