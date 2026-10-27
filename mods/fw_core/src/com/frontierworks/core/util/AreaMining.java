package com.frontierworks.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Helpers for 3x3 mining (hammers, excavators, drills) and vein mining, with recursion protection. */
public final class AreaMining {
    private static boolean busy;

    private AreaMining() {}

    public static boolean isBusy() {
        return busy;
    }

    /** Face of the block the player is looking at (for orienting the 3x3 plane). */
    public static Direction face(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(6));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : Direction.UP;
    }

    /** The 8 surrounding positions in the plane perpendicular to face, at the given radius. */
    public static List<BlockPos> plane(BlockPos center, Direction face, int radius) {
        List<BlockPos> out = new ArrayList<>();
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p;
                switch (face.getAxis()) {
                    case X: p = center.offset(0, a, b); break;
                    case Y: p = center.offset(a, 0, b); break;
                    default: p = center.offset(a, b, 0); break;
                }
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Breaks each position as the player would (drops, events, tool damage), if canBreak allows it.
     * Returns number broken.
     */
    public static int breakAll(ServerPlayer player, List<BlockPos> positions, Predicate<BlockState> canBreak, float maxHardness) {
        if (busy) return 0;
        busy = true;
        int n = 0;
        try {
            Level level = player.level();
            for (BlockPos p : positions) {
                BlockState st = level.getBlockState(p);
                if (st.isAir()) continue;
                float h = st.getDestroySpeed(level, p);
                if (h < 0 || h > maxHardness || !canBreak.test(st)) continue;
                ItemStack tool = player.getMainHandItem();
                if (tool.isEmpty()) break;
                if (player.gameMode.destroyBlock(p)) n++;
            }
        } finally {
            busy = false;
        }
        return n;
    }

    /** Connected blocks of the same type (vein / tree), breadth-first, up to limit. */
    public static List<BlockPos> connected(Level level, BlockPos start, Predicate<BlockState> same, int limit, boolean diagonals) {
        List<BlockPos> out = new ArrayList<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && out.size() < limit) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (!diagonals && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                        BlockPos np = p.offset(dx, dy, dz);
                        if (seen.add(np) && same.test(level.getBlockState(np))) {
                            out.add(np);
                            queue.add(np);
                            if (out.size() >= limit) return out;
                        }
                    }
                }
            }
        }
        return out;
    }
}
