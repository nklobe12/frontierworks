package com.lodestar.petroleum;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.multiblock.MultiblockPattern;
import com.lodestar.core.test.SelfTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/** Functional checks for the server self-test: pumping, distillation by column height, cracking, plastics, fuels and reservoirs. */
final class PetroleumTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private PetroleumTests() {}

    static void register() {
        SelfTest.scenario("petroleum:pumpjack", 220, PetroleumTests::pumpjack);
        SelfTest.scenario("petroleum:distillation", 220, (l, o) -> distillation(l, o, 5));
        SelfTest.scenario("petroleum:short_column", 220, (l, o) -> distillation(l, o, 2));
        SelfTest.scenario("petroleum:cracker", 300, PetroleumTests::cracker);
        SelfTest.scenario("petroleum:polymerizer", 160, PetroleumTests::polymerizer);
        SelfTest.scenario("petroleum:diesel_dynamo", 120, PetroleumTests::dynamo);
        SelfTest.scenario("petroleum:reservoirs", 1, PetroleumTests::reservoirs);
    }

    private static void floor(ServerLevel l, BlockPos o) {
        for (int x = -4; x <= 8; x++)
            for (int z = -8; z <= 8; z++) l.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
    }

    private static <T extends BlockEntity> T place(ServerLevel l, BlockPos p, Block b, Direction facing) {
        BlockState st = b.defaultBlockState();
        if (st.hasProperty(LSEntityBlock.FACING)) st = st.setValue(LSEntityBlock.FACING, facing);
        l.setBlock(p, st, 3);
        @SuppressWarnings("unchecked") T be = (T) l.getBlockEntity(p);
        return be;
    }

    private static IFluidHandler fluids(ServerLevel l, BlockPos p, Direction side) {
        BlockEntity be = l.getBlockEntity(p);
        return be == null ? null : be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).resolve().orElse(null);
    }

    private static int amount(IFluidHandler h, Fluid f) {
        if (h == null) return -1;
        int n = 0;
        for (int i = 0; i < h.getTanks(); i++) if (h.getFluidInTank(i).getFluid().isSame(f)) n += h.getFluidInTank(i).getAmount();
        return n;
    }

    private static Supplier<String> pumpjack(ServerLevel l, BlockPos o) {
        floor(l, o);
        Direction facing = Direction.SOUTH;
        MultiblockPattern pat = PumpjackBE.pattern();
        PumpjackBE ctrl = place(l, o, Content.PUMPJACK.get(), facing);
        BlockPos well = null;
        for (int y = 0; y < pat.height; y++)
            for (int z = 0; z < pat.depth; z++)
                for (int x = 0; x < pat.width; x++) {
                    char c = pat.charAt(x, y, z);
                    BlockPos p = pat.toWorld(o, facing, x, y, z);
                    switch (c) {
                        case 'F' -> l.setBlock(p, Content.PUMPJACK_BASE.get().defaultBlockState(), 3);
                        case 'P' -> l.setBlock(p, Content.PUMPJACK_POST.get().defaultBlockState(), 3);
                        case 'W' -> {
                            l.setBlock(p, Content.WELLHEAD.get().defaultBlockState(), 3);
                            well = p;
                        }
                        case '_' -> l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        default -> {}
                    }
                }
        final BlockPos wellPos = well;
        ChunkPos chunk = new ChunkPos(wellPos);
        OilField.override(l, chunk, 100_000);
        ctrl.getEnergy().setEnergy(40000);
        return () -> {
            int crude = ctrl.getTank(0).getFluidAmount();
            int left = OilField.remaining(l, chunk);
            FluidStack fromWell = fluids(l, wellPos, Direction.UP) == null ? FluidStack.EMPTY
                    : fluids(l, wellPos, Direction.UP).drain(100, IFluidHandler.FluidAction.SIMULATE);
            LOG.info("petroleum:pumpjack formed={} status={} crude={} mB, reservoir left={} mB, wellhead offers {} mB",
                    ctrl.isFormed(), ctrl.status(), crude, left, fromWell.getAmount());
            if (!ctrl.isFormed()) return "pumpjack did not form (status " + ctrl.status() + ")";
            if (crude < PumpjackBE.STROKE_MB * 2) return "pumped only " + crude + " mB";
            if (left != 100_000 - crude) return "reservoir did not shrink by what was pumped (" + left + ")";
            if (fromWell.getAmount() <= 0) return "wellhead does not expose the oil";
            return null;
        };
    }

    private static Supplier<String> distillation(ServerLevel l, BlockPos o, int trays) {
        floor(l, o);
        DistillationBE col = place(l, o, Content.DISTILLATION_COLUMN.get(), Direction.NORTH);
        for (int i = 1; i <= trays; i++) l.setBlock(o.above(i), Content.DISTILLATION_TRAY.get().defaultBlockState(), 3);
        l.setBlock(o.above(trays + 1), Blocks.AIR.defaultBlockState(), 3);
        col.getTank(0).fill(new FluidStack(Content.CRUDE_OIL.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
        col.getEnergy().setEnergy(30000);
        return () -> {
            int[] got = new int[DistillationBE.MAX_TRAYS];
            for (int i = 0; i < DistillationBE.MAX_TRAYS; i++) got[i] = col.getTank(1 + i).getFluidAmount();
            int viaTray = trays > 0 ? amount(fluids(l, o.above(trays), Direction.EAST), DistillationBE.FRACTIONS[trays - 1].get()) : 0;
            LOG.info("petroleum:distillation {} trays: crude left={} heavy={} diesel={} kerosene={} naphtha={} gas={}, top tray offers {} mB, tray state={}",
                    col.trays(), col.getTank(0).getFluidAmount(), got[0], got[1], got[2], got[3], got[4], viaTray,
                    l.getBlockState(o.above(trays)).getValue(TrayBlock.FRACTION));
            if (col.trays() != trays) return "column counted " + col.trays() + " trays";
            for (int i = 0; i < DistillationBE.MAX_TRAYS; i++) {
                if (i < trays && got[i] <= 0) return "tray " + (i + 1) + " collected nothing";
                if (i >= trays && got[i] > 0) return "fraction " + (i + 1) + " collected without its tray";
            }
            if (got[0] * 25 != got[1] * 30) return "fraction ratio wrong: " + got[0] + "/" + got[1];
            if (viaTray <= 0) return "top tray does not expose its fraction";
            return null;
        };
    }

    private static Supplier<String> cracker(ServerLevel l, BlockPos o) {
        floor(l, o);
        MachineBlockEntity m = place(l, o, Content.CRACKER.get(), Direction.NORTH);
        IFluidHandler h = fluids(l, o, null);
        int a = h.fill(new FluidStack(Content.HEAVY_OIL.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
        int b = h.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        m.getEnergy().setEnergy(30000);
        return () -> {
            int diesel = amount(h, Content.DIESEL.get()), gas = amount(h, Content.REFINERY_GAS.get());
            LOG.info("petroleum:cracker accepted heavy={} water={}, made diesel={} gas={}, heavy left={}", a, b, diesel, gas, amount(h, Content.HEAVY_OIL.get()));
            if (a < 2000 || b < 2000) return "cracker refused its inputs";
            if (gas < 60) return "made only " + gas + " mB refinery gas";
            return diesel >= 100 ? null : "made only " + diesel + " mB diesel";
        };
    }

    private static Supplier<String> polymerizer(ServerLevel l, BlockPos o) {
        floor(l, o);
        MachineBlockEntity m = place(l, o, Content.POLYMERIZER.get(), Direction.NORTH);
        m.getTank(0).fill(new FluidStack(Content.REFINERY_GAS.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        m.getEnergy().setEnergy(20000);
        return () -> {
            IItemHandler items = m.getItems();
            int pellets = 0;
            Item want = Content.PLASTIC_PELLETS.get();
            for (int i = 0; i < items.getSlots(); i++) if (items.getStackInSlot(i).is(want)) pellets += items.getStackInSlot(i).getCount();
            LOG.info("petroleum:polymerizer made {} plastic pellets, gas left={}", pellets, m.getTank(0).getFluidAmount());
            return pellets >= 2 ? null : "made " + pellets + " pellets";
        };
    }

    private static Supplier<String> dynamo(ServerLevel l, BlockPos o) {
        floor(l, o);
        Block dyn = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ls_dynamos", "compression_dynamo"));
        if (dyn == null || dyn == Blocks.AIR) return () -> null;
        l.setBlock(o, dyn.defaultBlockState().setValue(LSEntityBlock.FACING_ALL, Direction.UP), 3);
        IFluidHandler h = fluids(l, o, null);
        int filled = h == null ? 0 : h.fill(new FluidStack(Content.DIESEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        // crude oil has no id entry in the dynamo's fuel table: it only burns through the forge:crude_oil tag
        BlockPos o2 = o.east(2);
        l.setBlock(o2, dyn.defaultBlockState().setValue(LSEntityBlock.FACING_ALL, Direction.UP), 3);
        IFluidHandler h2 = fluids(l, o2, null);
        int crude = h2 == null ? 0 : h2.fill(new FluidStack(Content.CRUDE_OIL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        return () -> {
            BlockEntity be2 = l.getBlockEntity(o2);
            IEnergyStorage es2 = be2 == null ? null : be2.getCapability(ForgeCapabilities.ENERGY, null).resolve().orElse(null);
            LOG.info("petroleum:diesel_dynamo accepted {} mB crude by tag, stored {} FE", crude, es2 == null ? -1 : es2.getEnergyStored());
            if (crude <= 0) return "compression dynamo refused crude oil (forge:crude_oil tag not honoured)";
            if (es2 == null || es2.getEnergyStored() <= 0) return "compression dynamo made no energy from tagged crude oil";
            return dieselCheck(l, o, filled);
        };
    }

    private static String dieselCheck(ServerLevel l, BlockPos o, int filled) {
        BlockEntity be = l.getBlockEntity(o);
        IEnergyStorage es = be == null ? null : be.getCapability(ForgeCapabilities.ENERGY, null).resolve().orElse(null);
        int stored = es == null ? 0 : es.getEnergyStored();
        LOG.info("petroleum:diesel_dynamo accepted {} mB diesel, stored {} FE", filled, stored);
        if (filled <= 0) return "compression dynamo refused diesel";
        return stored > 0 ? null : "compression dynamo made no energy from diesel";
    }

    private static Supplier<String> reservoirs(ServerLevel l, BlockPos o) {
        long seed = l.getSeed();
        int oil = 0, total = 0;
        long sum = 0;
        int max = 0;
        for (int cx = -60; cx < 60; cx++)
            for (int cz = -60; cz < 60; cz++) {
                total++;
                int a = OilField.initialFromSeed(seed, cx, cz);
                if (a > 0) {
                    oil++;
                    sum += a;
                    max = Math.max(max, a);
                }
            }
        final int n = oil, t = total, mx = max;
        final long avg = oil > 0 ? sum / oil : 0;
        return () -> {
            double frac = (double) n / t;
            LOG.info("petroleum:reservoirs {} of {} chunks hold oil ({}%), average {} B, largest {} B", n, t, Math.round(frac * 100), avg / 1000, mx / 1000);
            if (frac < 0.08 || frac > 0.4) return "oil in " + Math.round(frac * 100) + "% of chunks";
            if (mx > OilField.MAX_AMOUNT * 1.15 || avg < OilField.MIN_AMOUNT) return "reservoir sizes out of range";
            return null;
        };
    }
}
