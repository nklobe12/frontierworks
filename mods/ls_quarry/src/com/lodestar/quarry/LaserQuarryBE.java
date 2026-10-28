package com.lodestar.quarry;

import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Laser quarry: stands on a tower of laser masts and cuts a square around the tower without a frame. Each mast adds two
 * blocks of reach (range augments add four, eight or twelve more, up to 24). The column under the tower is never dug.
 */
public class LaserQuarryBE extends AbstractQuarryBE {
    public static final MachineLayout LAYOUT = layout(true);
    public static final int MIN_MASTS = 2, MAX_RADIUS = 24;

    private int radius = -1;
    private int masts;

    public LaserQuarryBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 200_000, 8_000);
    }

    @Override
    protected int ticksPerBlock() {
        return 3;
    }

    @Override
    protected int baseCost() {
        return 64;
    }

    public int countMasts() {
        int n = 0;
        BlockPos p = worldPosition.below();
        while (n < 64 && level.getBlockState(p).is(Content.LASER_MAST.get())) {
            n++;
            p = p.below();
        }
        return n;
    }

    public int maxRadius() {
        return Math.min(MAX_RADIUS, masts * 2 + 4 * Math.round(augment(AugmentItem.Kind.RANGE)));
    }

    public void setRadius(int r) {
        radius = r;
    }

    @Override
    protected boolean defineArea() {
        masts = countMasts();
        if (masts < MIN_MASTS) {
            status = ST_NO_TOWER;
            return false;
        }
        int r = radius < 0 ? maxRadius() : Math.max(1, Math.min(radius, maxRadius()));
        radius = r;
        x0 = worldPosition.getX() - r;
        x1 = worldPosition.getX() + r;
        z0 = worldPosition.getZ() - r;
        z1 = worldPosition.getZ() + r;
        topY = worldPosition.getY() - 1;
        return true;
    }

    @Override
    protected boolean skipColumn(int x, int z) {
        return x == worldPosition.getX() && z == worldPosition.getZ();
    }

    @Override
    protected boolean tickUpkeep() {
        if (level.getGameTime() % 40 == 0) masts = countMasts();
        if (masts < MIN_MASTS) {
            status = ST_NO_TOWER;
            return false;
        }
        return true;
    }

    @Override
    public void serverTick() {
        if (level != null && phase == Phase.NO_AREA && level.getGameTime() % 20 == 0) {
            masts = countMasts();
            if (masts < MIN_MASTS) status = ST_NO_TOWER;
        }
        super.serverTick();
        if (phase == Phase.NO_AREA && masts < MIN_MASTS) status = ST_NO_TOWER;
    }

    @Override
    public void onCustomButton(int id, Player player) {
        boolean idle = phase == Phase.NO_AREA || phase == Phase.DONE;
        if ((id == BTN_MINUS || id == BTN_PLUS) && idle) {
            int cur = radius < 0 ? maxRadius() : radius;
            radius = Math.max(1, Math.min(Math.max(1, maxRadius()), cur + (id == BTN_PLUS ? 1 : -1)));
            if (phase == Phase.DONE) reset();
            return;
        }
        super.onCustomButton(id, player);
    }

    @Override
    public int getExtra(int i) {
        if (i == X_SIZE_A && !hasArea) return radius < 0 ? maxRadius() : radius;
        if (i == X_SIZE_B && !hasArea) return maxRadius();
        if (i == X_MASTS) return masts;
        return super.getExtra(i);
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        super.addInfo(lines, menu);
        if (menu.extra(X_PHASE) == Phase.NO_AREA.ordinal()) {
            while (lines.size() > 1) lines.remove(1);
            lines.add(Component.translatable("gui.ls_quarry.tower", menu.extra(X_MASTS), menu.extra(X_SIZE_A), menu.extra(X_SIZE_B)));
        }
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putInt("Radius", radius);
        tag.putInt("Masts", masts);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        radius = tag.contains("Radius") ? tag.getInt("Radius") : -1;
        masts = tag.getInt("Masts");
    }
}
