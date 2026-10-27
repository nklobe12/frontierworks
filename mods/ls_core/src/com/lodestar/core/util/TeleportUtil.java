package com.lodestar.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class TeleportUtil {
    private TeleportUtil() {}

    public static ResourceKey<Level> dimKey(String id) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(id));
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server, String id) {
        return server.getLevel(dimKey(id));
    }

    /** Teleports any entity (players included) to an exact position, across dimensions if needed. */
    public static Entity teleport(Entity entity, ServerLevel dest, double x, double y, double z, float yaw, float pitch) {
        entity.fallDistance = 0;
        if (entity instanceof ServerPlayer) {
            ServerPlayer p = (ServerPlayer) entity;
            p.teleportTo(dest, x, y, z, yaw, pitch);
            return p;
        }
        if (entity.level() == dest) {
            entity.teleportTo(x, y, z);
            return entity;
        }
        Entity moved = entity.changeDimension(dest, new ExactTeleporter(new Vec3(x, y, z), yaw, pitch));
        return moved == null ? entity : moved;
    }

    /**
     * Finds a safe standing position near (x,z): the surface for open-sky worlds, otherwise the first
     * two-block air gap above solid ground scanning from the top of the logical height down.
     */
    public static BlockPos findSafe(ServerLevel level, int x, int z, @Nullable BlockState platform) {
        level.getChunk(x >> 4, z >> 4);
        if (!level.dimensionType().hasCeiling()) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y > level.getMinBuildHeight() + 1) {
                BlockPos ground = new BlockPos(x, y - 1, z);
                if (level.getBlockState(ground).getFluidState().isEmpty()) {
                    return new BlockPos(x, y, z);
                }
                if (platform != null) {
                    level.setBlockAndUpdate(ground, platform);
                    return new BlockPos(x, y, z);
                }
            }
        } else {
            int top = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 2;
            for (int y = top; y > level.getMinBuildHeight() + 2; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (level.isEmptyBlock(p) && level.isEmptyBlock(p.above()) && level.getBlockState(p.below()).isSolidRender(level, p.below())) {
                    return p;
                }
            }
        }
        int y = Math.max(level.getSeaLevel() + 1, level.getMinBuildHeight() + 64);
        BlockPos p = new BlockPos(x, y, z);
        BlockState base = platform != null ? platform : Blocks.COBBLESTONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(p.offset(dx, -1, dz), base);
                level.setBlockAndUpdate(p.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(p.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
            }
        }
        return p;
    }

    public static class ExactTeleporter implements ITeleporter {
        private final Vec3 pos;
        private final float yaw, pitch;

        public ExactTeleporter(Vec3 pos, float yaw, float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
            return repositionEntity.apply(false);
        }

        @Nullable
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(pos, Vec3.ZERO, yaw, pitch);
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
            return false;
        }
    }
}
