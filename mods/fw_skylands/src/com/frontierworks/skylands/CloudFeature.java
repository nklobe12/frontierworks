package com.frontierworks.skylands;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A puffy cloud made of a few overlapping ellipsoids, placed only into open air. */
public class CloudFeature extends Feature<NoneFeatureConfiguration> {
    public CloudFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos o = ctx.origin();
        RandomSource r = ctx.random();
        BlockState cloud = Content.CLOUD.get().defaultBlockState();
        boolean any = false;
        int blobs = 2 + r.nextInt(3);
        for (int b = 0; b < blobs; b++) {
            BlockPos c = o.offset(r.nextInt(9) - 4, r.nextInt(2), r.nextInt(9) - 4);
            int rx = 2 + r.nextInt(4), ry = 1 + r.nextInt(2), rz = 2 + r.nextInt(4);
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-rx, -ry, -rz), c.offset(rx, ry, rz))) {
                double dx = (p.getX() - c.getX()) / (double) rx;
                double dy = (p.getY() - c.getY()) / (double) ry;
                double dz = (p.getZ() - c.getZ()) / (double) rz;
                if (dx * dx + dy * dy + dz * dz <= 1.0 && level.isEmptyBlock(p)) {
                    level.setBlock(p, cloud, 2);
                    any = true;
                }
            }
        }
        return any;
    }
}
