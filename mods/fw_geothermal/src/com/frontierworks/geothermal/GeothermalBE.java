package com.frontierworks.geothermal;

import com.frontierworks.core.machine.GeneratorBE;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/** Taps heat from adjacent lava (sources count fully, flowing lava and magma blocks partly). Nether bonus. */
public class GeothermalBE extends GeneratorBE {
    public static final MachineLayout LAYOUT = MachineLayout.generator(false, 0, 0);
    private final int perSource;
    private int checkTimer;
    private int cached;

    public GeothermalBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int perSource) {
        super(type, pos, state, LAYOUT, perSource * 10000, perSource * 24);
        this.perSource = perSource;
    }

    @Override
    protected int generate() {
        if (level == null) return 0;
        if (checkTimer-- <= 0) {
            checkTimer = 40;
            float total = 0;
            for (Direction d : Direction.values()) {
                BlockPos p = worldPosition.relative(d);
                BlockState st = level.getBlockState(p);
                if (level.getFluidState(p).getType().isSame(Fluids.LAVA)) {
                    total += level.getFluidState(p).isSource() ? perSource : perSource * 0.4f;
                } else if (st.is(Blocks.MAGMA_BLOCK)) {
                    total += perSource * 0.25f;
                }
            }
            if (level.dimension() == Level.NETHER) total *= 1.5f;
            cached = Math.round(total);
        }
        return cached;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        lines.add(Component.translatable("gui.fw_geothermal.tip"));
    }
}
