package com.frontierworks.hydro;

import com.frontierworks.core.machine.GeneratorBE;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/** Generates power from moving water on its sides (flowing water counts double, still water a little). */
public class WaterWheelBE extends GeneratorBE {
    public static final MachineLayout LAYOUT = MachineLayout.generator(false, 0, 0);
    private final int perFace;
    private int checkTimer;
    private int cached;

    public WaterWheelBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int perFace) {
        super(type, pos, state, LAYOUT, perFace * 8000, perFace * 24);
        this.perFace = perFace;
    }

    @Override
    protected int generate() {
        if (level == null) return 0;
        if (checkTimer-- <= 0) {
            checkTimer = 40;
            int total = 0;
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                FluidState fs = level.getFluidState(worldPosition.relative(d));
                if (fs.getType().isSame(Fluids.WATER)) {
                    total += fs.isSource() ? perFace / 4 : perFace;
                }
            }
            cached = total;
        }
        return cached;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        lines.add(Component.translatable("gui.fw_hydro.tip"));
    }
}
