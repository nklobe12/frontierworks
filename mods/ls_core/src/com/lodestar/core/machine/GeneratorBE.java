package com.lodestar.core.machine;

import com.lodestar.core.energy.EnergyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

/**
 * Base for energy producers. Subclasses implement {@link #generate()} returning FE made this tick;
 * the base pushes stored energy to every neighbour that accepts it and charges CHARGE slots.
 */
public abstract class GeneratorBE extends MachineBlockEntity {
    protected final int pushRate;
    protected int lastGenerated;

    protected GeneratorBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout, int capacity, int pushRate) {
        super(type, pos, state, layout, capacity, 0, pushRate);
        this.pushRate = pushRate;
    }

    /** Produce energy for this tick. Called only when storage is not full. */
    protected abstract int generate();

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        int made = 0;
        if (!energy.isFull() && canRun()) {
            made = generate();
            if (made > 0) {
                made = Math.round(made * (1 + outputBonus()));
                energy.produce(made);
            }
        } else {
            idle();
        }
        lastGenerated = made;
        rate = made;
        chargeItems(pushRate);
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, pushRate, null);
        setLit(made > 0);
    }

    /** Called instead of generate() while full. */
    protected void idle() {}

    @Override
    protected IEnergyStorage getEnergyForCap() {
        return energy;
    }

    @Override
    public int getExtra(int i) {
        return i == 0 ? lastGenerated : 0;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_core.generating", menu.extra(0)));
    }
}
