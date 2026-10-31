package com.lodestar.petroleum;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.multiblock.IMultiblockController;
import com.lodestar.core.multiblock.MultiblockHelper;
import com.lodestar.core.multiblock.MultiblockPattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Pumpjack motor. With its frame built in front of it (deck of pumpjack bases, samson post, wellhead) and power, the
 * walking beam nods and every stroke lifts crude oil out of the reservoir in the chunk under the wellhead until it is
 * dry. The oil collects in the motor's tank; pipes can take it from the motor or the wellhead.
 */
public class PumpjackBE extends MachineBlockEntity implements IMultiblockController {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 52).tank(30, 17, 16, 52, 8000, false, true).info(54, 20)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .extra(6).build();
    public static final int STROKE_MB = 200, BASE_PERIOD = 40, BASE_FE = 48;
    public static final int S_IDLE = 0, S_PUMPING = 1, S_UNFORMED = 2, S_NO_OIL = 3, S_DEPLETED = 4, S_FULL = 5, S_NO_POWER = 6;

    private static MultiblockPattern pattern;

    public static MultiblockPattern pattern() {
        if (pattern == null) {
            pattern = MultiblockPattern.builder("ls_petroleum:pumpjack")
                    .layer("FCF", "FFF", "FFF", "FFF", " W ")
                    .layer(" _ ", " _ ", " P ", " _ ", " _ ")
                    .layer(" _ ", " _ ", " P ", " _ ", " _ ")
                    .layer(" _ ", " _ ", " _ ", " _ ", " _ ")
                    .key('F', () -> Content.PUMPJACK_BASE.get())
                    .key('P', () -> Content.PUMPJACK_POST.get())
                    .key('W', () -> Content.WELLHEAD.get())
                    .build();
        }
        return pattern;
    }

    private final MultiblockHelper mb = new MultiblockHelper(pattern(), 40);
    private final LazyOptional<IFluidHandler> portCap = LazyOptional.of(() -> new DrainOnly(tanks[0]));
    private int stroke;
    private int status = S_UNFORMED;
    private int remaining;
    private int lookup;
    @Nullable
    private BlockPos problem;
    // client view
    public boolean running;
    public boolean formed;
    public int period = BASE_PERIOD;
    public float phase, prevPhase;

    public PumpjackBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 40000, 1000, 0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(Content.CRUDE_OIL.get());
    }

    public int strokePeriod() {
        return Math.max(8, (int) Math.round(BASE_PERIOD * speedMultiplier()));
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean changed = mb.tick(level, worldPosition, facing());
        if (changed && mb.formed()) lookup = 0;
        boolean active = false;
        int newPeriod = strokePeriod();
        if (!mb.formed()) {
            status = S_UNFORMED;
            if (level.getGameTime() % 20 == 0) problem = pattern().firstProblem(level, worldPosition, facing());
        } else {
            problem = null;
            BlockPos well = mb.positions('W').get(0);
            ChunkPos chunk = new ChunkPos(well);
            ServerLevel sl = (ServerLevel) level;
            if (--lookup <= 0) {
                lookup = 20;
                remaining = OilField.remaining(sl, chunk);
            }
            int cost = (int) Math.max(1, Math.round(BASE_FE * energyMultiplier()));
            if (remaining <= 0) {
                status = OilField.initial(sl, chunk) > 0 ? S_DEPLETED : S_NO_OIL;
            } else if (tanks[0].getSpace() < STROKE_MB) {
                status = S_FULL;
            } else if (!canRun()) {
                status = S_IDLE;
            } else if (energy == null || energy.getEnergyStored() < cost) {
                status = S_NO_POWER;
            } else {
                status = S_PUMPING;
                energy.consume(cost);
                rate = cost;
                active = true;
                if (++stroke >= newPeriod) {
                    stroke = 0;
                    int want = Math.round(STROKE_MB * (1 + outputBonus()));
                    int got = OilField.extract(sl, chunk, want);
                    remaining = Math.max(0, remaining - got);
                    if (got > 0) {
                        tanks[0].fill(new FluidStack(Content.CRUDE_OIL.get(), got), IFluidHandler.FluidAction.EXECUTE);
                        level.playSound(null, well, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.25f, 0.5f);
                    }
                }
                progress = stroke;
                maxProgress = newPeriod;
            }
        }
        if (!active) rate = 0;
        if (active != running || mb.formed() != formed || newPeriod != period) {
            running = active;
            formed = mb.formed();
            period = newPeriod;
            sync();
        }
        setLit(active);
    }

    @Override
    public void clientTick() {
        prevPhase = phase;
        if (running && period > 0) {
            phase += 1f / period;
            if (phase >= 1f) {
                phase -= 1f;
                prevPhase -= 1f;
            }
        }
    }

    public int status() {
        return status;
    }

    public int reservoir() {
        return remaining;
    }

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case 0 -> status;
            case 1 -> remaining / 1000;
            case 2 -> problem == null ? 0 : 1;
            case 3 -> problem == null ? 0 : problem.getX();
            case 4 -> problem == null ? 0 : problem.getY();
            default -> problem == null ? 0 : problem.getZ();
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int s = menu.extra(0);
        String key = switch (s) {
            case S_PUMPING -> "pumping";
            case S_UNFORMED -> "not_formed";
            case S_NO_OIL -> "no_oil";
            case S_DEPLETED -> "depleted";
            case S_FULL -> "full";
            case S_NO_POWER -> "no_power";
            default -> "idle";
        };
        lines.add(Component.translatable("gui.ls_petroleum." + key).withStyle(s == S_PUMPING ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
        if (s == S_UNFORMED && menu.extra(2) == 1) {
            lines.add(Component.translatable("gui.ls_petroleum.missing", menu.extra(3), menu.extra(4), menu.extra(5)));
        } else if (s != S_UNFORMED && s != S_NO_OIL) {
            lines.add(Component.translatable("gui.ls_petroleum.reservoir", String.format("%,d", menu.extra(1))));
        }
    }

    // ------------------------------------------------------------------ multiblock ports

    @Override
    public boolean isFormed() {
        return mb.formed();
    }

    @Override
    public <T> LazyOptional<T> getPortCapability(Capability<T> cap, BlockPos portPos, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && mb.formed()) return portCap.cast();
        return LazyOptional.empty();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        portCap.invalidate();
    }

    private boolean unloading;

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && !unloading) mb.release(level);
        super.setRemoved();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Stroke", stroke);
        mb.save(tag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        stroke = tag.getInt("Stroke");
        mb.load(tag);
    }

    @Override
    protected void saveSyncExtra(CompoundTag tag) {
        tag.putBoolean("Running", running);
        tag.putBoolean("Formed", formed);
        tag.putInt("Period", period);
    }

    @Override
    protected void loadSyncExtra(CompoundTag tag) {
        running = tag.getBoolean("Running");
        formed = tag.getBoolean("Formed");
        period = Math.max(1, tag.getInt("Period"));
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5);
    }

    /** Lets pipes take fluid out of a tank but never put any in. */
    public static class DrainOnly implements IFluidHandler {
        private final FluidTank tank;

        public DrainOnly(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int t) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int t, @Nonnull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !tank.getFluid().isFluidEqual(resource)) return FluidStack.EMPTY;
            return tank.drain(resource.getAmount(), action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
