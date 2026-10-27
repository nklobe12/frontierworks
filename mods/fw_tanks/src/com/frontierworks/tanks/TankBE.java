package com.frontierworks.tanks;

import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Fluid tank: fill/drain with buckets or ducts, drains into the tank below, keeps contents when broken. */
public class TankBE extends MachineBlockEntity {
    public TankBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, MachineLayout.builder().tank(62, 17, 52, 52, capacity, true, true).info(118, 30).build(), 0, 0, 0);
    }

    @Override
    public void serverTick() {
        if (level == null || tanks[0].isEmpty() || level.getGameTime() % 4 != 0) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof TankBE) {
            IFluidHandler h = below.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
            if (h != null) {
                int filled = h.fill(tanks[0].getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) tanks[0].drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int cap = layout.tanks.get(0).capacity;
        lines.add(Component.literal(menu.tankAmount(0) * 100L / Math.max(1, cap) + "%"));
    }
}
