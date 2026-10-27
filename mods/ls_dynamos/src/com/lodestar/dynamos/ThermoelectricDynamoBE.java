package com.lodestar.dynamos;

import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Thermoelectric Dynamo: no fuel at all. It makes power from the temperature difference between hot blocks
 * (lava, magma, fire, campfires) and cold blocks (water, ice, snow) touching its other faces.
 */
public class ThermoelectricDynamoBE extends DynamoBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(156, 17, 52).slot(130, 53, MachineLayout.Role.CHARGE)
            .slot(8, 17, MachineLayout.Role.UPGRADE).slot(8, 35, MachineLayout.Role.UPGRADE).slot(8, 53, MachineLayout.Role.UPGRADE)
            .info(30, 20).extra(2).build();

    private int hot, cold;

    public ThermoelectricDynamoBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 60);
    }

    @Override
    protected long consumeFuel() {
        if (level.getGameTime() % 20 == 0 || (hot == 0 && cold == 0)) scan();
        return hot > 0 && cold > 0 ? 1000 : 0;
    }

    @Override
    protected int currentRate() {
        return Math.min(baseRate, Math.min(hot, cold) * 6 + Math.max(hot, cold));
    }

    private void scan() {
        hot = 0;
        cold = 0;
        for (Direction d : Direction.values()) {
            if (d == coil()) continue;
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.getFluidState().is(FluidTags.LAVA)) hot += s.getFluidState().isSource() ? 4 : 2;
            else if (s.is(Blocks.MAGMA_BLOCK)) hot += 2;
            else if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) hot += 1;
            else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) hot += 1;
            else if (s.getFluidState().is(FluidTags.WATER)) cold += s.getFluidState().isSource() ? 2 : 1;
            else if (s.is(Blocks.BLUE_ICE)) cold += 5;
            else if (s.is(Blocks.PACKED_ICE)) cold += 4;
            else if (s.is(Blocks.ICE)) cold += 3;
            else if (s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) cold += 1;
        }
    }

    @Override
    public int getExtra(int i) {
        return i == 0 ? hot : cold;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        lines.add(Component.translatable("gui.ls_dynamos.hot", menu.extra(0)));
        lines.add(Component.translatable("gui.ls_dynamos.cold", menu.extra(1)));
    }
}
