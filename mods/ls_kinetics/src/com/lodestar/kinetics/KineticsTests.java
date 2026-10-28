package com.lodestar.kinetics;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.test.SelfTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/** Functional checks for the server self-test: every source, the gearing maths, overload, belts and each machine. */
final class KineticsTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private KineticsTests() {}

    static void register() {
        SelfTest.scenario("kinetics:mill", 220, KineticsTests::mill);
        SelfTest.scenario("kinetics:gearing", 120, KineticsTests::gearing);
        SelfTest.scenario("kinetics:overload", 120, KineticsTests::overload);
        SelfTest.scenario("kinetics:belt", 220, KineticsTests::belt);
        SelfTest.scenario("kinetics:vat", 240, KineticsTests::vat);
        SelfTest.scenario("kinetics:blower", 260, KineticsTests::blower);
        SelfTest.scenario("kinetics:paddle_wheel", 160, KineticsTests::paddle);
        SelfTest.scenario("kinetics:windmill", 100, KineticsTests::windmill);
        SelfTest.scenario("kinetics:hand_wheel", 80, KineticsTests::handWheel);
    }

    // ------------------------------------------------------------------ helpers

    private static void floor(ServerLevel l, BlockPos o, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) l.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
    }

    private static BlockState axis(Block b, Direction.Axis a) {
        return b.defaultBlockState().setValue(KineticAxisBlock.AXIS, a);
    }

    private static BlockState facingAll(Block b, Direction d) {
        return b.defaultBlockState().setValue(LSEntityBlock.FACING_ALL, d);
    }

    private static BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(LSEntityBlock.FACING, d);
    }

    private static MotorBE motor(ServerLevel l, BlockPos p, Direction f, int speedIdx) {
        l.setBlock(p, facingAll(Content.KINETIC_MOTOR.get(), f), 3);
        MotorBE m = (MotorBE) l.getBlockEntity(p);
        m.energy().setEnergy(40000);
        m.setTarget(speedIdx, false);
        return m;
    }

    private static int outputs(KineticMachineBE be, Item item) {
        int n = 0;
        for (int s : be.getLayout().indices(MachineLayout.Role.OUTPUT)) {
            ItemStack st = be.getItems().getStackInSlot(s);
            if (st.is(item)) n += st.getCount();
        }
        return n;
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));
    }

    // ------------------------------------------------------------------ scenarios

    /** Motor -> axles -> bevel junction -> axle up -> grist mill grinding wheat. */
    private static Supplier<String> mill(ServerLevel l, BlockPos o) {
        floor(l, o, -1, 5, -1, 1);
        motor(l, o, Direction.EAST, 2);
        l.setBlock(o.east(1), axis(Content.AXLE.get(), Direction.Axis.X), 3);
        l.setBlock(o.east(2), axis(Content.AXLE.get(), Direction.Axis.X), 3);
        l.setBlock(o.east(3), Content.BEVEL_JUNCTION.get().defaultBlockState(), 3);
        l.setBlock(o.east(3).above(), axis(Content.AXLE.get(), Direction.Axis.Y), 3);
        BlockPos mp = o.east(3).above(2);
        l.setBlock(mp, facing(Content.GRIST_MILL.get(), Direction.NORTH), 3);
        KineticMachineBE mill = (KineticMachineBE) l.getBlockEntity(mp);
        ItemHandlerHelper.insertItem(mill.getItems(), new ItemStack(Items.WHEAT, 8), false);
        return () -> {
            int flour = outputs(mill, Content.FLOUR.get());
            LOG.info("kinetics:mill shaft {} RPM, {} flour from 8 wheat", mill.rpm(), flour);
            return flour >= 2 ? null : "grist mill made " + flour + " flour at " + mill.rpm() + " RPM";
        };
    }

    /** Motor at 64 RPM -> gear train 2:1 -> tachometer -> dynamo: the dynamo turns at 128 RPM and makes 64 FE/t. */
    private static Supplier<String> gearing(ServerLevel l, BlockPos o) {
        floor(l, o, -1, 5, -1, 1);
        motor(l, o, Direction.EAST, 2);
        l.setBlock(o.east(1), facingAll(Content.GEAR_TRAIN.get(), Direction.EAST), 3);
        ((GearTrainBE) l.getBlockEntity(o.east(1))).configure(3, false);
        l.setBlock(o.east(2), axis(Content.TACHOMETER.get(), Direction.Axis.X), 3);
        l.setBlock(o.east(3), facing(Content.KINETIC_DYNAMO.get(), Direction.EAST), 3);
        AxleBE tach = (AxleBE) l.getBlockEntity(o.east(2));
        KineticDynamoBE dyn = (KineticDynamoBE) l.getBlockEntity(o.east(3));
        return () -> {
            int fe = dyn.getEnergy().getEnergyStored();
            LOG.info("kinetics:gearing tachometer {} RPM, dynamo {} RPM, {} FE stored, comparator {}", tach.speed(), dyn.rpm(), fe, tach.comparatorOutput());
            if (Math.abs(Math.abs(dyn.rpm()) - 128) > 0.5f) return "dynamo turns at " + dyn.rpm() + " RPM, expected 128";
            if (Math.abs(Math.abs(tach.speed()) - 128) > 0.5f) return "tachometer reads " + tach.speed();
            return fe > 0 ? null : "dynamo made no energy";
        };
    }

    /** Motor (512 Nm) geared up 2:1 into three dynamos (3 x 128 Nm x 2 = 768 Nm): the network must stall. */
    private static Supplier<String> overload(ServerLevel l, BlockPos o) {
        floor(l, o, -1, 5, -2, 2);
        motor(l, o, Direction.EAST, 1);
        l.setBlock(o.east(1), facingAll(Content.GEAR_TRAIN.get(), Direction.EAST), 3);
        ((GearTrainBE) l.getBlockEntity(o.east(1))).configure(3, false);
        BlockPos j = o.east(2);
        l.setBlock(j, Content.BEVEL_JUNCTION.get().defaultBlockState(), 3);
        l.setBlock(j.east(), facing(Content.KINETIC_DYNAMO.get(), Direction.EAST), 3);
        l.setBlock(j.north(), facing(Content.KINETIC_DYNAMO.get(), Direction.NORTH), 3);
        l.setBlock(j.south(), facing(Content.KINETIC_DYNAMO.get(), Direction.SOUTH), 3);
        BevelJunctionBE jb = (BevelJunctionBE) l.getBlockEntity(j);
        KineticDynamoBE d = (KineticDynamoBE) l.getBlockEntity(j.east());
        return () -> {
            KineticGrid g = jb.currentGrid();
            float dem = g == null ? -1 : g.demand, cap = g == null ? -1 : g.capacity;
            LOG.info("kinetics:overload status {} demand {} Nm capacity {} Nm, dynamo {} FE", jb.status(), dem, cap, d.getEnergy().getEnergyStored());
            if (jb.status() != KineticBE.OVERLOADED) return "network not overloaded (status " + jb.status() + ", " + dem + "/" + cap + " Nm)";
            return d.getEnergy().getEnergyStored() == 0 ? null : "an overloaded dynamo still made energy";
        };
    }

    /** Motor -> pulley ~belt~ pulley -> stamping press making iron plates. */
    private static Supplier<String> belt(ServerLevel l, BlockPos o) {
        floor(l, o, -1, 7, -1, 1);
        motor(l, o, Direction.UP, 2);
        BlockPos a = o.above(), b = o.above().east(5);
        l.setBlock(a, axis(Content.PULLEY.get(), Direction.Axis.Y), 3);
        l.setBlock(b, axis(Content.PULLEY.get(), Direction.Axis.Y), 3);
        PulleyBE pa = (PulleyBE) l.getBlockEntity(a), pb = (PulleyBE) l.getBlockEntity(b);
        String why = pa.canLink(pb);
        if (why != null) return () -> "pulleys cannot be belted: " + why;
        pa.link(pb);
        l.setBlock(o.east(5), facing(Content.STAMPING_PRESS.get(), Direction.NORTH), 3);
        KineticMachineBE press = (KineticMachineBE) l.getBlockEntity(o.east(5));
        ItemHandlerHelper.insertItem(press.getItems(), new ItemStack(Items.IRON_INGOT, 6), false);
        Item plate = item("ls_metalwork:iron_plate");
        return () -> {
            int n = outputs(press, plate);
            LOG.info("kinetics:belt far pulley {} RPM, press {} RPM, {} iron plates", pb.speed(), press.rpm(), n);
            return n >= 1 ? null : "press made " + n + " plates (far pulley at " + pb.speed() + " RPM)";
        };
    }

    /** A vat mixing mud from dirt and water, and a heated vat over lava mixing molten bronze from ingots. */
    private static Supplier<String> vat(ServerLevel l, BlockPos o) {
        floor(l, o, -2, 6, -2, 2);
        BlockPos v1 = o, v2 = o.east(4).above();
        motor(l, v1.above(), Direction.DOWN, 2);
        l.setBlock(v1, facing(Content.MIXING_VAT.get(), Direction.NORTH), 3);
        KineticMachineBE plain = (KineticMachineBE) l.getBlockEntity(v1);
        plain.getTank(0).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        ItemHandlerHelper.insertItem(plain.getItems(), new ItemStack(Items.DIRT, 2), false);
        // lava pit under the second vat
        BlockPos lava = v2.below();
        for (Direction d : Direction.Plane.HORIZONTAL) l.setBlock(lava.relative(d), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(lava, Blocks.LAVA.defaultBlockState(), 3);
        l.setBlock(v2, facing(Content.MIXING_VAT.get(), Direction.NORTH), 3);
        motor(l, v2.above(), Direction.DOWN, 3);
        KineticMachineBE hot = (KineticMachineBE) l.getBlockEntity(v2);
        ItemHandlerHelper.insertItem(hot.getItems(), new ItemStack(Items.COPPER_INGOT, 3), false);
        ItemHandlerHelper.insertItem(hot.getItems(), new ItemStack(item("ls_veins:tin_ingot"), 1), false);
        return () -> {
            int mud = outputs(plain, Items.MUD);
            FluidStack molten = hot.getTank(2).getFluid();
            LOG.info("kinetics:vat mud {} (water left {} mB), heated={} output {} mB of {}", mud, plain.getTank(0).getFluidAmount(), hot.heated(),
                    molten.getAmount(), molten.isEmpty() ? "nothing" : molten.getFluid().getFluidType().getDescriptionId());
            if (mud < 1) return "vat made no mud";
            if (!hot.heated()) return "vat over lava is not heated";
            return molten.getAmount() >= 576 ? null : "heated vat made " + molten.getAmount() + " mB";
        };
    }

    /** Air blower through a campfire roasts raw beef; a second blower through soul fire steeps sand into soul sand. */
    private static Supplier<String> blower(ServerLevel l, BlockPos o) {
        BlockPos[] lines = {o, o.south(4)};
        for (int k = 0; k < 2; k++) {
            BlockPos s = lines[k];
            floor(l, s, -1, 8, -1, 1);
            motor(l, s, Direction.EAST, 2);
            l.setBlock(s.east(1), facingAll(Content.AIR_BLOWER.get(), Direction.EAST), 3);
            if (k == 0) {
                l.setBlock(s.east(2), Blocks.CAMPFIRE.defaultBlockState(), 3);
            } else {
                l.setBlock(s.east(2).below(), Blocks.SOUL_SAND.defaultBlockState(), 3);
                l.setBlock(s.east(2), Blocks.SOUL_FIRE.defaultBlockState(), 3);
            }
            l.setBlock(s.east(6), Blocks.STONE.defaultBlockState(), 3);
            ItemEntity ie = new ItemEntity(l, s.getX() + 4.5, s.getY() + 0.2, s.getZ() + 0.5, new ItemStack(k == 0 ? Items.BEEF : Items.SAND, 4));
            ie.setDeltaMovement(0, 0, 0);
            ie.setPickUpDelay(32767);
            l.addFreshEntity(ie);
        }
        AirBlowerBE b0 = (AirBlowerBE) l.getBlockEntity(o.east(1));
        return () -> {
            AABB box0 = new AABB(o).inflate(8, 3, 2), box1 = new AABB(o.south(4)).inflate(8, 3, 2);
            int cooked = 0, soul = 0, raw = 0;
            for (ItemEntity e : l.getEntitiesOfClass(ItemEntity.class, box0)) {
                if (e.getItem().is(Items.COOKED_BEEF)) cooked += e.getItem().getCount();
                if (e.getItem().is(Items.BEEF)) raw += e.getItem().getCount();
                e.discard();
            }
            for (ItemEntity e : l.getEntitiesOfClass(ItemEntity.class, box1)) {
                if (e.getItem().is(Items.SOUL_SAND)) soul += e.getItem().getCount();
                e.discard();
            }
            LOG.info("kinetics:blower {} RPM stream {} blocks ({}), cooked beef {} (raw left {}), soul sand {}", b0.rpm(), b0.range(), b0.catalyst(), cooked, raw, soul);
            if (cooked < 4) return "roasting stream cooked " + cooked + " beef (raw left " + raw + ", range " + b0.range() + ")";
            return soul >= 4 ? null : "soul stream made " + soul + " soul sand";
        };
    }

    /** Water flowing down a channel under a paddle wheel turns it and the tachometer on its axle. */
    private static Supplier<String> paddle(ServerLevel l, BlockPos o) {
        floor(l, o, -2, 9, -2, 2);
        for (int x = -1; x <= 8; x++) {
            l.setBlock(o.offset(x, 0, -1), Blocks.STONE.defaultBlockState(), 3);
            l.setBlock(o.offset(x, 0, 1), Blocks.STONE.defaultBlockState(), 3);
        }
        l.setBlock(o.offset(-1, 0, 0), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(o.offset(8, 0, 0), Blocks.STONE.defaultBlockState(), 3);
        BlockPos wheel = o.offset(3, 1, 0);
        l.setBlock(wheel, axis(Content.PADDLE_WHEEL.get(), Direction.Axis.Z), 3);
        l.setBlock(wheel.south(), axis(Content.TACHOMETER.get(), Direction.Axis.Z), 3);
        l.setBlock(o, Blocks.WATER.defaultBlockState(), 3);
        PaddleWheelBE pw = (PaddleWheelBE) l.getBlockEntity(wheel);
        AxleBE tach = (AxleBE) l.getBlockEntity(wheel.south());
        return () -> {
            LOG.info("kinetics:paddle_wheel wheel {} RPM from {} wet paddles (blocked={}), tachometer {} RPM", pw.sourceSpeed(), pw.wetCells(), pw.obstructed(), tach.speed());
            if (pw.sourceSpeed() == 0) return "paddle wheel does not turn (wet " + pw.wetCells() + ", blocked " + pw.obstructed() + ")";
            return Math.abs(tach.speed()) > 0 ? null : "tachometer on the wheel's axle reads 0";
        };
    }

    /** A 15-vane sail raised high up turns its hub and the tachometer behind it. */
    private static Supplier<String> windmill(ServerLevel l, BlockPos o) {
        BlockPos hub = o.above(150);
        l.setBlock(hub.below(), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(hub, facing(Content.WINDMILL_HUB.get(), Direction.NORTH), 3);
        l.setBlock(hub.south(), axis(Content.TACHOMETER.get(), Direction.Axis.Z), 3);
        WindmillHubBE h = (WindmillHubBE) l.getBlockEntity(hub);
        for (int u = -2; u <= 2; u++)
            for (int v = -1; v <= 1; v++) l.setBlock(h.cell(u, v), Content.WINDMILL_VANE.get().defaultBlockState(), 3);
        String err = h.assemble();
        if (err != null) return () -> "windmill did not assemble: " + err;
        AxleBE tach = (AxleBE) l.getBlockEntity(hub.south());
        return () -> {
            LOG.info("kinetics:windmill {} vanes, wind {}, {} RPM {} Nm, tachometer {} RPM", h.vanes().size(), h.wind(), h.sourceSpeed(), h.sourceTorque(), tach.speed());
            if (h.vanes().size() != 15) return "sail has " + h.vanes().size() + " vanes";
            if (h.sourceSpeed() == 0) return "windmill does not turn (wind " + h.wind() + ", blocked " + h.obstructed() + ")";
            return Math.abs(tach.speed()) > 0 ? null : "tachometer behind the hub reads 0";
        };
    }

    /** A hand wheel kept turning drives an axle at 24 RPM. */
    private static Supplier<String> handWheel(ServerLevel l, BlockPos o) {
        floor(l, o, -1, 1, -1, 1);
        l.setBlock(o, axis(Content.TACHOMETER.get(), Direction.Axis.Y), 3);
        l.setBlock(o.above(), facingAll(Content.HAND_WHEEL.get(), Direction.UP), 3);
        HandWheelBE w = (HandWheelBE) l.getBlockEntity(o.above());
        w.turn(false, 400);
        AxleBE tach = (AxleBE) l.getBlockEntity(o);
        return () -> {
            LOG.info("kinetics:hand_wheel tachometer {} RPM", tach.speed());
            return Math.abs(Math.abs(tach.speed()) - HandWheelBE.RPM) < 0.5f ? null : "tachometer reads " + tach.speed() + " RPM";
        };
    }
}
