package com.lodestar.aeolus;

import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Tidal generator. Works only submerged in an ocean biome with open water around it (7x7, two blocks deep). Output follows the
 * tidal current through the day ({@link Tides}) and the phase of the moon. Output augments raise it.
 */
public class TidalGeneratorBE extends MachineBlockEntity {
    public static final int OK = 0, NOT_OCEAN = 1, NOT_SUBMERGED = 2, SHALLOW = 3;
    public static final int PEAK = 80, FULL_WATER = 70, MIN_WATER = 20;
    public static final MachineLayout LAYOUT = MachineLayout.builder().slot(130, 53, MachineLayout.Role.CHARGE).energy(156, 17, 52)
            .slot(8, 17, MachineLayout.Role.UPGRADE).slot(8, 35, MachineLayout.Role.UPGRADE)
            .info(28, 52).extra(4).build();
    private static final int CAPACITY = 40_000, PUSH = 500;

    public int status = NOT_OCEAN;
    public int water;
    private int checkTimer = 1000;

    public float targetSpeed, speed, angle, prevAngle;
    private float syncedSpeed = Float.NaN;

    public TidalGeneratorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, CAPACITY, 0, PUSH);
    }

    @Override
    public boolean hasSideConfig() {
        return false;
    }

    @Override
    public boolean acceptsAugment(AugmentItem a) {
        return a.kind == AugmentItem.Kind.OUTPUT || a.kind == AugmentItem.Kind.CAPACITY;
    }

    public void checkSite() {
        water = 0;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++)
                for (int dy = 0; dy <= 1; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    FluidState fs = level.getFluidState(worldPosition.offset(dx, dy, dz));
                    if (fs.isSource() && fs.is(FluidTags.WATER)) water++;
                }
        if (!level.getBiome(worldPosition).is(BiomeTags.IS_OCEAN)) status = NOT_OCEAN;
        else if (!level.getFluidState(worldPosition.above()).is(FluidTags.WATER)) status = NOT_SUBMERGED;
        else if (water < MIN_WATER) status = SHALLOW;
        else status = OK;
    }

    public double waterFactor() {
        return Math.min(1.0, water / (double) FULL_WATER);
    }

    /** Energy per tick right now. */
    public int output() {
        if (status != OK) return 0;
        double f = Tides.current(level.getDayTime()) * Tides.moon(level.getMoonPhase()) * waterFactor() * (1 + outputBonus());
        return (int) Math.round(PEAK * f);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        if (++checkTimer >= 40) {
            checkTimer = 0;
            checkSite();
        }
        int made = 0;
        if (canRun() && !energy.isFull()) {
            made = output();
            energy.produce(made);
        }
        rate = made;
        chargeItems(PUSH);
        Direction front = facing();
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, PUSH, d -> d != front);
        setLit(made > 0);
        float want = made <= 0 ? 0 : 2f + 14f * made / PEAK;
        if (Float.isNaN(syncedSpeed) || Math.abs(want - syncedSpeed) > 0.8f) {
            syncedSpeed = want;
            targetSpeed = Tides.rising(level.getDayTime()) ? want : -want;
            sync();
        }
        setChanged();
    }

    @Override
    public void clientTick() {
        speed += (targetSpeed - speed) * 0.04f;
        if (Math.abs(angle) > 3600f) angle -= Math.signum(angle) * 3600f;
        prevAngle = angle;
        angle += speed;
    }

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case 0 -> rate;
            case 1 -> status;
            case 2 -> (int) Math.round(waterFactor() * 100);
            default -> (int) Math.round(outputBonus() * 100);
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_aeolus.output", menu.extra(0)));
        switch (menu.extra(1)) {
            case NOT_OCEAN -> lines.add(Component.translatable("gui.ls_aeolus.status.not_ocean").withStyle(ChatFormatting.DARK_RED));
            case NOT_SUBMERGED -> lines.add(Component.translatable("gui.ls_aeolus.status.not_submerged").withStyle(ChatFormatting.DARK_RED));
            case SHALLOW -> lines.add(Component.translatable("gui.ls_aeolus.status.shallow").withStyle(ChatFormatting.DARK_RED));
            default -> lines.add(Component.translatable("gui.ls_aeolus.water", menu.extra(2)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Water", water);
        tag.putInt("Status", status);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        water = tag.getInt("Water");
        status = tag.getInt("Status");
    }

    @Override
    protected void saveSyncExtra(CompoundTag tag) {
        tag.putFloat("Speed", targetSpeed);
    }

    @Override
    protected void loadSyncExtra(CompoundTag tag) {
        targetSpeed = tag.getFloat("Speed");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }
}
