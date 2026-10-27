package com.lodestar.ducts;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.test.SelfTest;
import com.lodestar.ducts.transit.Transit;
import com.lodestar.ducts.transit.TransitPod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Functional checks for the server self-test: items, filters, energy, fluids and viaduct travel. */
final class DuctsTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private DuctsTests() {}

    static void register() {
        SelfTest.scenario("ducts:items", 140, DuctsTests::itemTransport);
        SelfTest.scenario("ducts:filter", 160, DuctsTests::filter);
        SelfTest.scenario("ducts:energy", 100, DuctsTests::energy);
        SelfTest.scenario("ducts:fluids", 80, DuctsTests::fluids);
        SelfTest.scenario("ducts:viaduct", 80, DuctsTests::viaduct);
    }

    private static Block block(String id) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
    }

    private static void floor(ServerLevel l, BlockPos o) {
        for (int x = -2; x <= 14; x++)
            for (int z = -3; z <= 3; z++) l.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
    }

    private static DuctBE duct(ServerLevel l, BlockPos p, String id) {
        DuctBlock b = (DuctBlock) block(id);
        l.setBlock(p, b.computeState(l, p, b.defaultBlockState()), 3);
        return (DuctBE) l.getBlockEntity(p);
    }

    private static IItemHandler items(ServerLevel l, BlockPos p) {
        BlockEntity be = l.getBlockEntity(p);
        return be == null ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
    }

    private static int count(ServerLevel l, BlockPos p, Item item) {
        IItemHandler h = items(l, p);
        int n = 0;
        if (h != null) for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).is(item)) n += h.getStackInSlot(i).getCount();
        return n;
    }

    private static void chest(ServerLevel l, BlockPos p, ItemStack... contents) {
        l.setBlock(p, Blocks.CHEST.defaultBlockState(), 3);
        IItemHandler h = items(l, p);
        for (ItemStack s : contents) ItemHandlerHelper.insertItem(h, s, false);
    }

    private static Supplier<String> itemTransport(ServerLevel l, BlockPos o) {
        floor(l, o);
        chest(l, o, new ItemStack(Items.COBBLESTONE, 32));
        for (int i = 1; i <= 3; i++) duct(l, o.east(i), "ls_ducts:itemduct");
        BlockPos dest = o.east(4);
        chest(l, dest);
        ((DuctBE) l.getBlockEntity(o.east())).attach(Direction.WEST, Attachment.Type.SERVO);
        return () -> {
            int n = count(l, dest, Items.COBBLESTONE);
            LOG.info("ducts:items delivered {} of 32 cobblestone", n);
            return n >= 8 ? null : "destination chest holds " + n + " cobblestone";
        };
    }

    private static Supplier<String> filter(ServerLevel l, BlockPos o) {
        floor(l, o);
        chest(l, o, new ItemStack(Items.COBBLESTONE, 16), new ItemStack(Items.DIRT, 16));
        for (int i = 1; i <= 4; i++) duct(l, o.east(i), "ls_ducts:itemduct");
        BlockPos north = o.east(3).north(), east = o.east(5);
        chest(l, north);
        chest(l, east);
        ((DuctBE) l.getBlockEntity(o.east())).attach(Direction.WEST, Attachment.Type.SERVO);
        DuctBE sorter = (DuctBE) l.getBlockEntity(o.east(3));
        sorter.attach(Direction.NORTH, Attachment.Type.FILTER);
        sorter.attachment(Direction.NORTH).filter[0] = new ItemStack(Items.DIRT);
        return () -> {
            int nd = count(l, north, Items.DIRT), nc = count(l, north, Items.COBBLESTONE);
            int ed = count(l, east, Items.DIRT), ec = count(l, east, Items.COBBLESTONE);
            LOG.info("ducts:filter north dirt={} cobble={}, east dirt={} cobble={}", nd, nc, ed, ec);
            if (nd == 0 || nc > 0 || ec == 0 || ed > 0) return "sorted wrongly: north dirt=" + nd + " cobble=" + nc + ", east dirt=" + ed + " cobble=" + ec;
            return null;
        };
    }

    private static Supplier<String> energy(ServerLevel l, BlockPos o) {
        floor(l, o);
        Block dyn = block("ls_dynamos:stirling_dynamo");
        Block furnace = block("ls_machina:electric_furnace");
        if (dyn == null || furnace == null || dyn == Blocks.AIR || furnace == Blocks.AIR) return () -> null;
        l.setBlock(o, dyn.defaultBlockState().setValue(LSEntityBlock.FACING_ALL, Direction.EAST), 3);
        ItemHandlerHelper.insertItem(items(l, o), new ItemStack(Items.COAL, 8), false);
        duct(l, o.east(1), "ls_ducts:fluxduct");
        duct(l, o.east(2), "ls_ducts:fluxduct");
        l.setBlock(o.east(3), furnace.defaultBlockState(), 3);
        BlockPos fp = o.east(3);
        return () -> {
            BlockEntity be = l.getBlockEntity(fp);
            IEnergyStorage e = be == null ? null : be.getCapability(ForgeCapabilities.ENERGY, Direction.WEST).resolve().orElse(null);
            int stored = e == null ? -1 : e.getEnergyStored();
            LOG.info("ducts:energy furnace holds {} FE", stored);
            return stored > 0 ? null : "electric furnace holds " + stored + " FE";
        };
    }

    private static Supplier<String> fluids(ServerLevel l, BlockPos o) {
        floor(l, o);
        Block dyn = block("ls_dynamos:magmatic_dynamo");
        if (dyn == null || dyn == Blocks.AIR) return () -> null;
        DuctBE first = duct(l, o, "ls_ducts:fluiduct");
        duct(l, o.east(1), "ls_ducts:fluiduct");
        duct(l, o.east(2), "ls_ducts:fluiduct");
        BlockPos dp = o.east(3);
        l.setBlock(dp, dyn.defaultBlockState().setValue(LSEntityBlock.FACING_ALL, Direction.EAST), 3);
        IFluidHandler in = first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).resolve().orElseThrow();
        int filled = 0;
        for (int i = 0; i < 5; i++) filled += in.fill(new FluidStack(Fluids.LAVA, 200), IFluidHandler.FluidAction.EXECUTE);
        final int total = filled;
        return () -> {
            BlockEntity be = l.getBlockEntity(dp);
            if (be == null) return "dynamo missing";
            int fluid = be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).map(h -> h.getFluidInTank(0).getAmount()).orElse(0);
            int energy = be.getCapability(ForgeCapabilities.ENERGY, Direction.EAST).map(IEnergyStorage::getEnergyStored).orElse(0);
            LOG.info("ducts:fluids filled {} mB, dynamo has {} mB and {} FE", total, fluid, energy);
            return total > 0 && (fluid > 0 || energy > 0) ? null : "filled " + total + " mB, dynamo has " + fluid + " mB and " + energy + " FE";
        };
    }

    private static Supplier<String> viaduct(ServerLevel l, BlockPos o) {
        floor(l, o);
        BlockPos a = o.above(), b = o.above().east(11);
        l.setBlock(a, block("ls_ducts:transit_station").defaultBlockState(), 3);
        Block tube = block("ls_ducts:viaduct");
        for (int i = 1; i <= 10; i++) {
            BlockPos p = a.east(i);
            l.setBlock(p, tube.getStateForPlacement(new net.minecraft.world.item.context.DirectionalPlaceContext(l, p, Direction.DOWN, ItemStack.EMPTY, Direction.UP)), 3);
        }
        l.setBlock(b, block("ls_ducts:transit_station").defaultBlockState(), 3);
        List<Transit.Route> routes = Transit.routes(l, a);
        if (routes.size() != 1) return () -> "expected one route, found " + routes.size();
        List<Vec3> pts = new ArrayList<>();
        for (BlockPos p : routes.get(0).path()) pts.add(Vec3.atCenterOf(p));
        TransitPod pod = new TransitPod(DuctsSetup.TRANSIT_POD.get(), l);
        pod.start(pts, b);
        l.addFreshEntity(pod);
        ArmorStand rider = new ArmorStand(EntityType.ARMOR_STAND, l);
        rider.moveTo(a.getX() + 0.5, a.getY() + 1, a.getZ() + 0.5);
        l.addFreshEntity(rider);
        rider.startRiding(pod, true);
        return () -> {
            Vec3 want = new Vec3(b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5);
            double d = rider.position().distanceTo(want);
            LOG.info("ducts:viaduct pod removed={} rider at {} ({} from target)", pod.isRemoved(), rider.position(), d);
            String r = !pod.isRemoved() ? "pod still travelling" : rider.isPassenger() ? "rider still seated" : d > 1.5 ? "rider ended " + d + " blocks from the far station" : null;
            rider.discard();
            return r;
        };
    }
}
