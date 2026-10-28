package com.lodestar.aeolus;

import com.lodestar.core.test.SelfTest;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Functional checks: turbines with clear, blocked and overlapping rotors; an overshot water wheel; tides; the anemometer. */
final class AeolusTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private AeolusTests() {}

    static void register() {
        SelfTest.scenario("aeolus:turbines", 160, AeolusTests::turbines);
        SelfTest.scenario("aeolus:water_wheel", 160, AeolusTests::waterWheel);
        SelfTest.scenario("aeolus:tidal", 120, AeolusTests::tidal);
        SelfTest.scenario("aeolus:anemometer", 60, AeolusTests::anemometer);
    }

    private static void fill(ServerLevel l, BlockPos a, BlockPos b, BlockState st) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (l.getBlockState(p) != st) l.setBlock(p, st, Block.UPDATE_CLIENTS);
        }
    }

    private static WindTurbineBE turbine(ServerLevel l, BlockPos p, boolean rotor) {
        l.setBlock(p, Content.WIND_TURBINE.get().defaultBlockState(), 3);
        WindTurbineBE be = (WindTurbineBE) l.getBlockEntity(p);
        if (rotor) be.getItems().setStackInSlot(WindTurbineBE.SLOT_ROTOR, new ItemStack(Content.STEEL_ROTOR.get()));
        return be;
    }

    private static Supplier<String> turbines(ServerLevel l, BlockPos o) {
        BlockPos base = o.above(100);
        fill(l, base.offset(-5, -5, -11), base.offset(34, 5, 11), Blocks.AIR.defaultBlockState());
        WindTurbineBE clear = turbine(l, base, true);
        WindTurbineBE blocked = turbine(l, base.east(12), true);
        l.setBlock(base.east(12).north().east().above(), Blocks.STONE.defaultBlockState(), 3);
        WindTurbineBE a = turbine(l, base.east(22), true);
        WindTurbineBE b = turbine(l, base.east(27), true);
        return () -> {
            int e = clear.getEnergy().getEnergyStored();
            int wear = clear.getItems().getStackInSlot(WindTurbineBE.SLOT_ROTOR).getDamageValue();
            LOG.info("aeolus:turbines clear turbine at y={} made {} FE (rate {} FE/t, clearance {}, rotor wear {}s); blocked status={} ({} blocks) energy={}; "
                            + "overlapping status={},{}", base.getY(), e, clear.getExtra(0), String.format("%.2f", clear.clearance), wear,
                    blocked.status, blocked.blocked, blocked.getEnergy().getEnergyStored(), a.status, b.status);
            if (clear.status != WindTurbineBE.OK || e <= 0) return "clear turbine status " + clear.status + " energy " + e;
            if (wear <= 0) return "rotor did not wear";
            if (blocked.status != WindTurbineBE.BLOCKED || blocked.getEnergy().getEnergyStored() != 0) return "blocked turbine status " + blocked.status;
            if (a.status != WindTurbineBE.COLLIDE || b.status != WindTurbineBE.COLLIDE) return "overlapping rotors not detected: " + a.status + "," + b.status;
            return null;
        };
    }

    private static Supplier<String> waterWheel(ServerLevel l, BlockPos o) {
        BlockPos w = o.above(3);
        fill(l, w.offset(-5, -3, -4), w.offset(5, 5, 4), Blocks.AIR.defaultBlockState());
        fill(l, w.offset(-5, -2, -4), w.offset(5, -2, 4), Blocks.STONE.defaultBlockState());
        l.setBlock(w, Content.WATER_WHEEL.get().defaultBlockState(), 3);
        // a spring above the east side, walled in so it can only fall down past the wheel (overshot wheel)
        for (BlockPos p : new BlockPos[]{w.offset(0, 2, 0), w.offset(2, 2, 0), w.offset(1, 2, -1), w.offset(1, 2, 1), w.offset(1, 3, 0)}) {
            l.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
        }
        l.setBlock(w.offset(1, 2, 0), Blocks.WATER.defaultBlockState(), 3);
        return () -> {
            WaterWheelBE be = (WaterWheelBE) l.getBlockEntity(w);
            if (be == null) return "water wheel missing";
            int e = be.getEnergy().getEnergyStored();
            LOG.info("aeolus:water_wheel moving water {} still {} torque {} -> {} FE/t, stored {} FE", be.moving, be.still,
                    String.format("%.2f", be.torque), be.output(), e);
            if (be.status != WaterWheelBE.OK) return "status " + be.status + " (blocked " + be.blocked + ", moving " + be.moving + ")";
            return e > 0 ? null : "no energy made, torque " + be.torque;
        };
    }

    private static Supplier<String> tidal(ServerLevel l, BlockPos o) {
        // the tide model itself
        if (Tides.current(0) > 0.01 || Tides.current(3000) < 0.99 || Math.abs(Tides.height(6000) + 1) > 1e-6 || Tides.moon(0) != 1.25 || Tides.moon(2) != 0.75) {
            return () -> "tide model is wrong";
        }
        // a generator here: works only if this happens to be an ocean
        BlockPos here = o.above(2);
        fill(l, here.offset(-4, -1, -4), here.offset(4, 3, 4), Blocks.STONE.defaultBlockState());
        fill(l, here.offset(-3, 0, -3), here.offset(3, 2, 3), Blocks.WATER.defaultBlockState());
        l.setBlock(here, Content.TIDAL_GENERATOR.get().defaultBlockState(), 3);
        final boolean hereOcean = l.getBiome(here).is(BiomeTags.IS_OCEAN);
        // and one in a real ocean
        Pair<BlockPos, Holder<Biome>> found = l.findClosestBiome3d(h -> h.is(BiomeTags.IS_OCEAN), o.atY(l.getSeaLevel()), 2400, 32, 64);
        BlockPos sea = null;
        if (found != null) {
            BlockPos f = found.getFirst();
            sea = new BlockPos(f.getX(), l.getSeaLevel() - 3, f.getZ());
            l.setChunkForced(sea.getX() >> 4, sea.getZ() >> 4, true);
            l.getChunk(sea);
            l.setBlock(sea, Content.TIDAL_GENERATOR.get().defaultBlockState(), 3);
        }
        final BlockPos seaPos = sea;
        return () -> {
            TidalGeneratorBE h = (TidalGeneratorBE) l.getBlockEntity(here);
            if (h == null) return "generator missing";
            double now = Tides.current(l.getDayTime());
            LOG.info("aeolus:tidal day time {} current {} moon phase {}; inland generator ocean={} status={} energy={}", l.getDayTime(),
                    String.format("%.2f", now), l.getMoonPhase(), hereOcean, h.status, h.getEnergy().getEnergyStored());
            if (!hereOcean && (h.status != TidalGeneratorBE.NOT_OCEAN || h.getEnergy().getEnergyStored() != 0)) return "generator outside an ocean ran";
            String r = null;
            if (seaPos != null) {
                TidalGeneratorBE s = (TidalGeneratorBE) l.getBlockEntity(seaPos);
                if (s == null) {
                    r = "ocean generator missing";
                } else {
                    LOG.info("aeolus:tidal ocean generator at {} status={} open water={} rate={} FE/t stored={} FE", seaPos, s.status, s.water,
                            s.output(), s.getEnergy().getEnergyStored());
                    if (s.status == TidalGeneratorBE.OK && now > 0.05 && s.getEnergy().getEnergyStored() <= 0) r = "ocean generator made nothing";
                    if (s.status == TidalGeneratorBE.NOT_OCEAN) r = "ocean generator thinks it is not in an ocean";
                }
                l.setChunkForced(seaPos.getX() >> 4, seaPos.getZ() >> 4, false);
            } else {
                LOG.info("aeolus:tidal no ocean within 2400 blocks; skipped the ocean generator");
            }
            return r;
        };
    }

    private static Supplier<String> anemometer(ServerLevel l, BlockPos o) {
        BlockPos p = o.above(100).west(8);
        fill(l, p.offset(-1, -1, -1), p.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        l.setBlock(p, Content.ANEMOMETER.get().defaultBlockState(), 3);
        return () -> {
            AnemometerBE be = (AnemometerBE) l.getBlockEntity(p);
            if (be == null) return "anemometer missing";
            double local = Wind.local(l, p);
            LOG.info("aeolus:anemometer wind {} m/s from {} -> comparator {}", String.format("%.1f", Wind.metres(local)),
                    Wind.compassKey(Wind.bearing(l)), be.comparatorOutput());
            return be.comparatorOutput() > 0 ? null : "comparator reads 0 at height " + p.getY();
        };
    }
}
