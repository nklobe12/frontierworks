package com.lodestar.petroleum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A pool of crude oil sunk into the ground and stained with tar sand, placed only over chunks whose reservoir is
 * rich enough (richness above the oil threshold plus a margin), so a seep always means oil below.
 */
public class OilSeepFeature extends Feature<OilSeepFeature.Config> {
    public record Config(BlockState fluid, BlockState stain, int size, float margin) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("fluid").forGetter(Config::fluid),
                BlockState.CODEC.fieldOf("stain").forGetter(Config::stain),
                Codec.INT.fieldOf("size").forGetter(Config::size),
                Codec.FLOAT.fieldOf("margin").forGetter(Config::margin)
        ).apply(i, Config::new));
    }

    public OilSeepFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        Config c = ctx.config();
        BlockPos origin = ctx.origin();
        if (OilField.richness(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4) < OilField.THRESHOLD + c.margin()) return false;
        int surface = origin.getY() - 1;
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.getFluidState().isEmpty() || !ground.isSolidRender(level, origin.below())) return false;
        double radius = c.size() * (0.6 + rand.nextDouble() * 0.5) + 0.5;
        int r = (int) Math.ceil(radius) + 1;
        // the ground must be roughly level
        for (int dx = -r; dx <= r; dx += Math.max(1, r)) {
            for (int dz = -r; dz <= r; dz += Math.max(1, r)) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz) - 1;
                if (Math.abs(h - surface) > 2) return false;
            }
        }
        double ox = rand.nextDouble() * 0.6 - 0.3, oz = rand.nextDouble() * 0.6 - 0.3;
        int depth = c.size() >= 3 ? 2 : 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean[][] pool = new boolean[2 * r + 1][2 * r + 1];
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                double wobble = 0.8 + 0.4 * rand.nextDouble();
                double d = Math.sqrt((dx + ox) * (dx + ox) + (dz + oz) * (dz + oz));
                pool[dx + r][dz + r] = d <= radius * wobble - 0.6 && Math.abs(dx) < r && Math.abs(dz) < r;
            }
        int placed = 0;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                if (pool[dx + r][dz + r]) {
                    // deeper in the middle
                    boolean deep = depth > 1 && isPool(pool, dx + r + 1, dz + r) && isPool(pool, dx + r - 1, dz + r)
                            && isPool(pool, dx + r, dz + r + 1) && isPool(pool, dx + r, dz + r - 1);
                    int bottom = surface - (deep ? 1 : 0);
                    for (int y = surface + 1; y <= surface + 3; y++) {
                        p.set(x, y, z);
                        BlockState above = level.getBlockState(p);
                        if (!above.isAir() && (above.is(BlockTags.REPLACEABLE) || above.canBeReplaced())) {
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    for (int y = bottom; y <= surface; y++) {
                        level.setBlock(p.set(x, y, z), c.fluid(), 2);
                        placed++;
                    }
                    stain(level, p.set(x, bottom - 1, z), c.stain());
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        for (int y = bottom; y <= surface; y++) {
                            BlockPos side = new BlockPos(x, y, z).relative(dir);
                            int sx = side.getX() - origin.getX() + r, sz = side.getZ() - origin.getZ() + r;
                            if (sx >= 0 && sz >= 0 && sx <= 2 * r && sz <= 2 * r && pool[sx][sz]) continue;
                            stain(level, side, c.stain());
                        }
                    }
                } else if (rand.nextFloat() < 0.35f && nearPool(pool, dx + r, dz + r)) {
                    stain(level, p.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z), c.stain());
                }
            }
        return placed > 0;
    }

    private static boolean isPool(boolean[][] pool, int x, int z) {
        return x >= 0 && z >= 0 && x < pool.length && z < pool.length && pool[x][z];
    }

    private static boolean nearPool(boolean[][] pool, int x, int z) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (isPool(pool, x + dx, z + dz)) return true;
        return false;
    }

    /** Replaces soil, sand, stone or loose blocks with the stain block, so the pool is sealed all round. */
    private static void stain(WorldGenLevel level, BlockPos p, BlockState stain) {
        BlockState st = level.getBlockState(p);
        if (st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.TERRACOTTA)
                || st.is(net.minecraft.world.level.block.Blocks.GRAVEL) || st.is(net.minecraft.world.level.block.Blocks.SANDSTONE)
                || st.isAir() || st.canBeReplaced()) {
            level.setBlock(p, stain, 2);
        }
    }
}
