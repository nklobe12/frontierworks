package com.frontierworks.fluidducts;

import com.frontierworks.core.block.PipeBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuctBE extends BlockEntity implements PipeBlockBase.Ticking {
    private int timer;

    public DuctBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null || !(getBlockState().getBlock() instanceof DuctBlock)) return;
        DuctBlock block = (DuctBlock) getBlockState().getBlock();
        if (block.rate <= 0 || ++timer < 5) return;
        timer = 0;
        if (level.hasNeighborSignal(worldPosition)) return;
        int budget = block.rate * 5;
        for (Direction d : Direction.values()) {
            BlockPos src = worldPosition.relative(d);
            BlockEntity be = level.getBlockEntity(src);
            if (be == null || be instanceof DuctBE) continue;
            IFluidHandler from = be.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).orElse(null);
            if (from == null) continue;
            FluidStack sim = from.drain(budget, IFluidHandler.FluidAction.SIMULATE);
            if (sim.isEmpty()) continue;
            List<IFluidHandler> dests = destinations(src);
            int accepted = push(sim, dests, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack real = from.drain(new FluidStack(sim, accepted), IFluidHandler.FluidAction.EXECUTE);
            push(real, dests, IFluidHandler.FluidAction.EXECUTE);
            budget -= real.getAmount();
            if (budget <= 0) return;
        }
    }

    private int push(FluidStack stack, List<IFluidHandler> dests, IFluidHandler.FluidAction action) {
        int remaining = stack.getAmount();
        for (IFluidHandler h : dests) {
            if (remaining <= 0) break;
            remaining -= h.fill(new FluidStack(stack, remaining), action);
        }
        return stack.getAmount() - remaining;
    }

    private List<IFluidHandler> destinations(BlockPos source) {
        List<IFluidHandler> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> hit = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        while (!queue.isEmpty() && seen.size() < 2048) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos np = p.relative(d);
                if (!level.isLoaded(np)) continue;
                BlockEntity be = level.getBlockEntity(np);
                if (be instanceof DuctBE) {
                    if (seen.add(np)) queue.add(np);
                } else if (be != null && !np.equals(source) && hit.add(np)) {
                    IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).orElse(null);
                    if (h != null) out.add(h);
                }
            }
        }
        return out;
    }
}
