package com.lodestar.quarry;

import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Frame quarry: takes its area from an adjacent survey marker (or a square behind itself), builds a steel frame around
 * it - a ring at its own level, a ring four blocks up and posts at the corners - and then drills the area out with the
 * gantry that rides the top ring. Frame blocks broken while it works are put back.
 */
public class QuarryBE extends AbstractQuarryBE {
    public static final MachineLayout LAYOUT = layout(false);
    public static final int FRAME_HEIGHT = 4;
    private static final int FRAME_COST = 40;

    private int frameIndex;
    private int repairIndex;
    /** area taken from markers, kept across restarts: x0, z0, x1, z1 (null = use the default square) */
    private int[] fixedArea;
    private List<BlockPos> frameCache;

    public QuarryBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 80_000, 2_000);
    }

    @Override
    protected int ticksPerBlock() {
        return 8;
    }

    @Override
    protected int baseCost() {
        return 40;
    }

    @Override
    protected boolean defineArea() {
        if (fixedArea == null) {
            MarkerBE marker = MarkerBE.adjacent(level, worldPosition);
            if (marker != null) {
                BlockPos a = marker.areaMin(), b = marker.areaMax();
                boolean inside = worldPosition.getX() >= a.getX() && worldPosition.getX() <= b.getX()
                        && worldPosition.getZ() >= a.getZ() && worldPosition.getZ() <= b.getZ();
                if (!inside && a != null && b != null) {
                    fixedArea = new int[]{a.getX() + 1, a.getZ() + 1, b.getX() - 1, b.getZ() - 1};
                    for (BlockPos p : marker.group()) {
                        if (p != null && level.getBlockEntity(p) instanceof MarkerBE) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                            deliver(new ItemStack(Content.SURVEY_MARKER.get()));
                        }
                    }
                }
            }
        }
        if (fixedArea != null) {
            x0 = fixedArea[0];
            z0 = fixedArea[1];
            x1 = fixedArea[2];
            z1 = fixedArea[3];
        } else {
            Direction back = facing().getOpposite();
            int size = 9 + 8 * Math.round(augment(AugmentItem.Kind.RANGE));
            int half = size / 2;
            BlockPos c = worldPosition.relative(back, 2 + half);
            x0 = c.getX() - half;
            x1 = c.getX() + half;
            z0 = c.getZ() - half;
            z1 = c.getZ() + half;
        }
        topY = worldPosition.getY();
        frameIndex = 0;
        frameCache = null;
        return true;
    }

    /** Ring at the quarry's level, ring FRAME_HEIGHT above it, and the four corner posts between. */
    public List<BlockPos> framePositions() {
        if (frameCache != null) return frameCache;
        List<BlockPos> out = new ArrayList<>();
        int ax = x0 - 1, bx = x1 + 1, az = z0 - 1, bz = z1 + 1;
        for (int y : new int[]{topY, topY + FRAME_HEIGHT}) {
            for (int x = ax; x <= bx; x++) {
                out.add(new BlockPos(x, y, az));
                out.add(new BlockPos(x, y, bz));
            }
            for (int z = az + 1; z < bz; z++) {
                out.add(new BlockPos(ax, y, z));
                out.add(new BlockPos(bx, y, z));
            }
        }
        for (int y = topY + 1; y < topY + FRAME_HEIGHT; y++) {
            out.add(new BlockPos(ax, y, az));
            out.add(new BlockPos(bx, y, az));
            out.add(new BlockPos(ax, y, bz));
            out.add(new BlockPos(bx, y, bz));
        }
        out.removeIf(p -> p.equals(worldPosition));
        frameCache = out;
        return out;
    }

    private boolean isFrame(BlockState st) {
        return st.is(Content.QUARRY_FRAME.get());
    }

    @Override
    protected boolean tickBuild() {
        List<BlockPos> frame = framePositions();
        int budget = (int) Math.round(1 + augment(AugmentItem.Kind.SPEED));
        for (int n = 0; n < 64 && budget > 0; n++) {
            if (frameIndex >= frame.size()) return true;
            BlockPos p = frame.get(frameIndex);
            if (!level.isLoaded(p)) return false;
            BlockState st = level.getBlockState(p);
            if (isFrame(st)) {
                frameIndex++;
                continue;
            }
            int cost = (int) Math.round(FRAME_COST * energyMultiplier());
            if (energy.getEnergyStored() < cost) {
                status = ST_NO_POWER;
                return false;
            }
            if (st.isAir() || st.canBeReplaced() || st.getBlock() instanceof LiquidBlock) {
                energy.consume(cost);
                level.setBlock(p, Content.QUARRY_FRAME.get().defaultBlockState(), 3);
                moveHead(p, 1);
                frameIndex++;
                budget--;
                continue;
            }
            if (st.is(IMMUNE) || st.hasBlockEntity() || st.getDestroySpeed(level, p) < 0) {
                phase = Phase.BLOCKED;
                head = p;
                sync();
                return false;
            }
            int dig = cost(st, p);
            if (energy.getEnergyStored() < dig) {
                status = ST_NO_POWER;
                return false;
            }
            energy.consume(dig);
            dig(p, st);
            budget--;
        }
        return frameIndex >= frame.size();
    }

    /** Puts back one missing frame block every few ticks. */
    @Override
    protected boolean tickUpkeep() {
        if (level.getGameTime() % 5 != 0) return true;
        List<BlockPos> frame = framePositions();
        if (frame.isEmpty()) return true;
        repairIndex = (repairIndex + 1) % frame.size();
        BlockPos p = frame.get(repairIndex);
        BlockState st = level.getBlockState(p);
        if (!isFrame(st) && (st.isAir() || st.canBeReplaced()) && energy.getEnergyStored() >= FRAME_COST) {
            energy.consume(FRAME_COST);
            level.setBlock(p, Content.QUARRY_FRAME.get().defaultBlockState(), 3);
        }
        return true;
    }

    @Override
    protected void onRestored() {
        for (BlockPos p : framePositions()) {
            if (isFrame(level.getBlockState(p))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected void reset() {
        super.reset();
        frameIndex = 0;
        frameCache = null;
    }

    public int frameTop() {
        return topY + FRAME_HEIGHT;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putInt("FrameIndex", frameIndex);
        if (fixedArea != null) tag.putIntArray("Fixed", fixedArea);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        frameIndex = tag.getInt("FrameIndex");
        fixedArea = tag.contains("Fixed") ? tag.getIntArray("Fixed") : null;
        if (fixedArea != null && fixedArea.length != 4) fixedArea = null;
        frameCache = null;
    }
}
