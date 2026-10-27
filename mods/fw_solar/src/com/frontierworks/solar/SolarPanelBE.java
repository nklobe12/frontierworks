package com.frontierworks.solar;

import com.frontierworks.core.machine.GeneratorBE;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Generates power from sunlight: full rate in clear daylight, reduced in rain; hybrid panels also work at night. */
public class SolarPanelBE extends GeneratorBE {
    public static final MachineLayout LAYOUT = MachineLayout.generator(false, 0, 0);
    private final int peak;
    private final float nightFactor;
    private int checkTimer;
    private boolean skyVisible;

    public SolarPanelBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int peak, float nightFactor) {
        super(type, pos, state, LAYOUT, peak * 2000, peak * 4);
        this.peak = peak;
        this.nightFactor = nightFactor;
    }

    @Override
    protected int generate() {
        if (level == null) return 0;
        if (checkTimer-- <= 0) {
            checkTimer = 40;
            skyVisible = level.canSeeSky(worldPosition.above());
        }
        if (!skyVisible) return 0;
        float f;
        if (level.isDay()) {
            f = 1.0f;
            if (level.isThundering()) f = 0.2f;
            else if (level.isRaining()) f = 0.5f;
        } else {
            f = nightFactor;
        }
        if (!level.dimensionType().hasSkyLight()) f = 0;
        return Math.round(peak * f);
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        lines.add(Component.translatable("gui.fw_solar.peak", peak));
    }
}
