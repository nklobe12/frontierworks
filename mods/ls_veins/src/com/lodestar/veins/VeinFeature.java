package com.lodestar.veins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Optional;

/**
 * A large ore vein: a thick, wandering tube of ore through stone. Mixes a primary ore with an optional secondary
 * ore, swaps to deepslate variants in deepslate, stays inside the 3x3 chunk area features may write to, and can
 * leave an outcrop on the surface above it as a hint.
 */
public class VeinFeature extends Feature<VeinFeature.Config> {
    public record Config(BlockState ore, BlockState deepOre, Optional<BlockState> secondary, Optional<BlockState> deepSecondary,
                         float secondaryChance, float density, int radius, int length, Optional<BlockState> indicator)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("ore").forGetter(Config::ore),
                BlockState.CODEC.fieldOf("deep_ore").forGetter(Config::deepOre),
                BlockState.CODEC.optionalFieldOf("secondary").forGetter(Config::secondary),
                BlockState.CODEC.optionalFieldOf("deep_secondary").forGetter(Config::deepSecondary),
                Codec.FLOAT.fieldOf("secondary_chance").forGetter(Config::secondaryChance),
                Codec.FLOAT.fieldOf("density").forGetter(Config::density),
                Codec.INT.fieldOf("radius").forGetter(Config::radius),
                Codec.INT.fieldOf("length").forGetter(Config::length),
                BlockState.CODEC.optionalFieldOf("indicator").forGetter(Config::indicator)
        ).apply(i, Config::new));
    }

    public VeinFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        Config c = ctx.config();
        BlockPos origin = ctx.origin();
        int ocx = origin.getX() >> 4, ocz = origin.getZ() >> 4;
        double x = origin.getX(), y = origin.getY(), z = origin.getZ();
        double yaw = rand.nextDouble() * Math.PI * 2;
        double pitch = (rand.nextDouble() - 0.5) * 0.7;
        int placed = 0;
        double midX = x, midZ = z;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < c.length(); i++) {
            double t = (double) i / Math.max(1, c.length() - 1);
            double r = c.radius() * (0.55 + 0.45 * Math.sin(Math.PI * t)) + rand.nextDouble() * 0.6;
            int ri = Mth.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dy = -ri; dy <= ri; dy++)
                    for (int dz = -ri; dz <= ri; dz++) {
                        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (d > r) continue;
                        // denser core, ragged edges
                        if (rand.nextFloat() > c.density() * (1.15 - 0.6 * d / r)) continue;
                        p.set(Mth.floor(x) + dx, Mth.floor(y) + dy, Mth.floor(z) + dz);
                        if (Math.abs((p.getX() >> 4) - ocx) > 1 || Math.abs((p.getZ() >> 4) - ocz) > 1) continue;
                        if (level.isOutsideBuildHeight(p)) continue;
                        BlockState here = level.getBlockState(p);
                        boolean secondary = c.secondary().isPresent() && rand.nextFloat() < c.secondaryChance();
                        BlockState put = null;
                        if (here.is(BlockTags.STONE_ORE_REPLACEABLES)) {
                            put = secondary ? c.secondary().get() : c.ore();
                        } else if (here.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                            put = secondary ? c.deepSecondary().orElse(c.secondary().get()) : c.deepOre();
                        }
                        if (put != null) {
                            level.setBlock(p, put, 2);
                            placed++;
                        }
                    }
            if (i == c.length() / 2) {
                midX = x;
                midZ = z;
            }
            x += Math.cos(yaw) * Math.cos(pitch);
            z += Math.sin(yaw) * Math.cos(pitch);
            y += Math.sin(pitch);
            yaw += (rand.nextDouble() - 0.5) * 0.5;
            pitch = Mth.clamp(pitch + (rand.nextDouble() - 0.5) * 0.3, -0.6, 0.6);
        }
        if (placed > 0 && c.indicator().isPresent() && rand.nextFloat() < 0.85f) {
            int ix = Mth.floor(midX), iz = Mth.floor(midZ);
            if (Math.abs((ix >> 4) - ocx) <= 1 && Math.abs((iz >> 4) - ocz) <= 1) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ix, iz);
                BlockPos at = new BlockPos(ix, top, iz);
                BlockState below = level.getBlockState(at.below());
                if (level.getBlockState(at).isAir() && below.isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                        && below.getFluidState().isEmpty()) {
                    level.setBlock(at, c.indicator().get(), 2);
                }
            }
        }
        return placed > 0;
    }
}
