package com.frontierworks.pump;

import com.frontierworks.core.item.UpgradeItem;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pumps fluid source blocks from below (water is treated as infinite and never removed). */
public class PumpBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(8, 17, 52).tank(80, 17, 16, 52, 16000, false, true)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).info(104, 30).build();
    private int timer;

    public PumpBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 50_000, 1_000, 0);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        push();
        int interval = Math.max(2, 20 - 6 * countUpgrades(UpgradeItem.Type.SPEED));
        if (++timer < interval) return;
        timer = 0;
        if (tanks[0].getSpace() < 1000 || energy.getEnergyStored() < 100) {
            setLit(false);
            return;
        }
        BlockPos src = findSource();
        if (src == null) {
            setLit(false);
            return;
        }
        FluidState fs = level.getFluidState(src);
        Fluid fluid = fs.getType();
        FluidStack stack = new FluidStack(fluid, 1000);
        if (tanks[0].fill(stack, IFluidHandler.FluidAction.SIMULATE) < 1000) return;
        if (!fluid.isSame(Fluids.WATER)) {
            BlockState st = level.getBlockState(src);
            if (st.getBlock() instanceof BucketPickup) {
                ((BucketPickup) st.getBlock()).pickupBlock(level, src, st);
            } else {
                level.setBlock(src, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        tanks[0].fill(stack, IFluidHandler.FluidAction.EXECUTE);
        energy.consume(100);
        setLit(true);
    }

    private BlockPos findSource() {
        BlockPos start = worldPosition.below();
        if (level.getFluidState(start).isEmpty()) return null;
        Fluid type = level.getFluidState(start).getType();
        int range = 16 + 16 * countUpgrades(UpgradeItem.Type.RANGE);
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        BlockPos best = null;
        while (!queue.isEmpty() && seen.size() < 4096) {
            BlockPos p = queue.poll();
            FluidState fs = level.getFluidState(p);
            if (fs.isSource() && (best == null || p.getY() >= best.getY())) best = p;
            for (Direction d : Direction.values()) {
                BlockPos np = p.relative(d);
                if (d == Direction.UP && np.getY() >= worldPosition.getY()) continue;
                if (np.distManhattan(worldPosition) > range || !seen.add(np)) continue;
                if (level.getFluidState(np).getType().isSame(type)) queue.add(np);
            }
        }
        return best;
    }

    private void push() {
        if (tanks[0].isEmpty()) return;
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(d));
            if (be == null) continue;
            IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).orElse(null);
            if (h == null) continue;
            int filled = h.fill(tanks[0].getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) tanks[0].drain(filled, IFluidHandler.FluidAction.EXECUTE);
            if (tanks[0].isEmpty()) return;
        }
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.fw_pump.tip"));
    }
}
