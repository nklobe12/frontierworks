package com.lodestar.kinetics;

import com.lodestar.core.machine.GeneratorBE;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Kinetic dynamo: a shaft into its back turns a generator. It loads the network with 128 Nm and makes
 * FE/t = RPM x 128 / 256 (half its shaft speed), pushed into every neighbour that takes energy.
 */
public class KineticDynamoBE extends GeneratorBE implements IKineticConsumer {
    public static final float TORQUE = 128;
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(156, 17, 52).slot(130, 53, MachineLayout.Role.CHARGE)
            .info(30, 20).extra(2).build();
    private float rpm;
    private byte driveStatus;
    private long lastDrive = -10;

    public KineticDynamoBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 50000, 1000);
    }

    @Override
    public boolean acceptsRotation(Direction side) {
        return side == facing().getOpposite();
    }

    @Override
    public float torqueDemand() {
        return TORQUE;
    }

    @Override
    public void setRotation(Direction side, float rpm, byte status) {
        this.rpm = rpm;
        this.driveStatus = status;
        if (level != null) lastDrive = level.getGameTime();
    }

    public float rpm() {
        return rpm;
    }

    @Override
    public void serverTick() {
        if (level != null && level.getGameTime() - lastDrive > 1) rpm = 0;
        super.serverTick();
    }

    @Override
    protected int generate() {
        return Math.round(Math.abs(rpm) * TORQUE / 256f);
    }

    @Override
    public int getExtra(int i) {
        return i == 0 ? lastGenerated : i == 1 ? driveStatus : 0;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        if (menu.extra(1) == KineticBE.OVERLOADED) lines.add(Component.translatable("gui.ls_kinetics.overloaded").withStyle(ChatFormatting.RED));
        else lines.add(Component.translatable("gui.ls_kinetics.dynamo_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
