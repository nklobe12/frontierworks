package com.frontierworks.wind;

import com.frontierworks.core.machine.GeneratorBE;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Wind turbine: output grows with altitude above sea level and with storms, and drops when the
 * 5x5x3 space above the turbine is obstructed.
 */
public class WindTurbineBE extends GeneratorBE {
    public static final MachineLayout LAYOUT = MachineLayout.generator(false, 0, 0);
    private final int max;
    private int checkTimer;
    private float clearance = 1.0f;

    public WindTurbineBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int max) {
        super(type, pos, state, LAYOUT, max * 2000, max * 4);
        this.max = max;
    }

    @Override
    protected int generate() {
        if (level == null) return 0;
        if (checkTimer-- <= 0) {
            checkTimer = 100;
            int blocked = 0, total = 0;
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    for (int dy = 1; dy <= 3; dy++) {
                        total++;
                        if (!level.isEmptyBlock(worldPosition.offset(dx, dy, dz))) blocked++;
                    }
            clearance = 1.0f - (float) blocked / total;
        }
        float height = Math.max(0, worldPosition.getY() - level.getSeaLevel()) / 96.0f;
        float weather = level.isThundering() ? 1.6f : level.isRaining() ? 1.25f : 1.0f;
        float f = Math.min(1.0f, height) * weather * clearance;
        // gentle gusting
        float gust = 0.85f + 0.15f * (float) Math.sin((level.getGameTime() + worldPosition.asLong()) / 200.0);
        return Math.round(max * Math.min(1.6f, f * gust));
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        lines.add(Component.translatable("gui.fw_wind.tip"));
    }
}
