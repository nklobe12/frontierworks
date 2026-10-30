package com.lodestar.datastore;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.test.SelfTest;
import com.lodestar.datastore.craft.CraftPlanner;
import com.lodestar.datastore.craft.Pattern;
import com.lodestar.datastore.storage.DiskData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Server self-test: storage through the API, importer/exporter/link, and autocrafting planks into sticks. */
final class DatastoreTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private DatastoreTests() {}

    static void register() {
        SelfTest.scenario("datastore:storage", 220, DatastoreTests::storage);
        SelfTest.scenario("datastore:autocraft", 220, DatastoreTests::autocraft);
        SelfTest.scenario("datastore:substitutes", 220, DatastoreTests::substitutes);
    }

    private static void floor(ServerLevel l, BlockPos o) {
        for (int x = -2; x <= 10; x++)
            for (int z = -4; z <= 3; z++) l.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
    }

    private static void cable(ServerLevel l, BlockPos p) {
        Block b = Content.DATA_CABLE.get();
        l.setBlock(p, Block.updateFromNeighbourShapes(b.defaultBlockState(), l, p), 3);
    }

    private static <T extends BlockEntity> T place(ServerLevel l, BlockPos p, Block b, Class<T> cls) {
        l.setBlock(p, b.defaultBlockState(), 3);
        return cls.cast(l.getBlockEntity(p));
    }

    private static <T extends BlockEntity> T port(ServerLevel l, BlockPos p, Block b, Class<T> cls) {
        l.setBlock(p, b.defaultBlockState().setValue(LSEntityBlock.FACING_ALL, Direction.NORTH), 3);
        return cls.cast(l.getBlockEntity(p));
    }

    private static IItemHandler chest(ServerLevel l, BlockPos p, ItemStack... contents) {
        l.setBlock(p, Blocks.CHEST.defaultBlockState(), 3);
        IItemHandler h = l.getBlockEntity(p).getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElseThrow();
        for (ItemStack s : contents) ItemHandlerHelper.insertItem(h, s, false);
        return h;
    }

    private static int count(IItemHandler h, Item item) {
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).is(item)) n += h.getStackInSlot(i).getCount();
        return n;
    }

    /** Core, cables and a drive bay with a copper disk, powered directly. */
    private static CoreBE network(ServerLevel l, BlockPos o, int cables) {
        floor(l, o);
        CoreBE core = place(l, o, Content.DATASTORE_CORE.get(), CoreBE.class);
        for (int i = 1; i <= cables; i++) cable(l, o.east(i));
        DriveBayBE drive = place(l, o.east(1).north(), Content.DRIVE_BAY.get(), DriveBayBE.class);
        drive.disks.setStackInSlot(0, new ItemStack(Content.COPPER_DATA_DISK.get()));
        return core;
    }

    private static Supplier<String> storage(ServerLevel l, BlockPos o) {
        CoreBE core = network(l, o, 6);
        ImporterBE imp = port(l, o.east(3).north(), Content.IMPORTER.get(), ImporterBE.class);
        IItemHandler src = chest(l, o.east(3).north(2), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIRT, 10));
        ExporterBE exp = port(l, o.east(4).north(), Content.EXPORTER.get(), ExporterBE.class);
        exp.filter.setItem(0, new ItemStack(Items.DIRT));
        IItemHandler dst = chest(l, o.east(4).north(2));
        port(l, o.east(5).north(), Content.INVENTORY_LINK.get(), LinkBE.class);
        IItemHandler linked = chest(l, o.east(5).north(2), new ItemStack(Items.DIAMOND, 5));
        place(l, o.east(6).north(), Content.ACCESS_TERMINAL.get(), TerminalBE.class);

        DataGrid g0 = core.grid();
        long offline = g0.insert(ItemKey.of(Items.IRON_INGOT), 10, false);
        if (offline != 0) return () -> "unpowered network accepted " + offline + " items";
        core.energy.setEnergy(CoreBE.CAPACITY);
        core.invalidateGrid();
        DataGrid g = core.grid();
        ItemKey iron = ItemKey.of(Items.IRON_INGOT);
        long in = g.insert(iron, 100, false);
        long out = g.extract(iron, 40, false);
        long left = g.count(iron);
        int devices = g.devices.size();
        LOG.info("datastore:storage devices={} status={} inserted {} iron, extracted {}, {} left", devices, g.status(), in, out, left);
        if (in != 100 || out != 40 || left != 60) return () -> "API insert/extract gave in=" + in + " out=" + out + " left=" + left;
        int startEnergy = core.energy.getEnergyStored();
        return () -> {
            DataGrid n = core.grid();
            long cobble = n.count(ItemKey.of(Items.COBBLESTONE));
            long diamonds = n.count(ItemKey.of(Items.DIAMOND));
            long more = n.insert(ItemKey.of(Items.DIAMOND), 3, false);
            int linkedDiamonds = count(linked, Items.DIAMOND);
            int exported = count(dst, Items.DIRT);
            int stillInSource = count(src, Items.COBBLESTONE);
            long[] disk = n.diskStats();
            ItemStack d = ((DriveBayBE) l.getBlockEntity(o.east(1).north())).disks.getStackInSlot(0);
            UUID id = DiskItem.id(d);
            boolean saved = id != null && DiskData.get(l.getServer()).has(id);
            int used = startEnergy - core.energy.getEnergyStored();
            LOG.info("datastore:storage cobble={} (source left {}), diamonds via link={} (+{} -> chest {}), dirt exported={}, disk bytes {}/{} types {}/{}, saved={}, upkeep {} FE/t, {} FE used",
                    cobble, stillInSource, diamonds, more, linkedDiamonds, exported, disk[0], disk[1], disk[2], disk[3], saved, n.upkeep(), used);
            if (cobble != 64 || stillInSource != 0) return "importer moved " + cobble + " cobblestone, " + stillInSource + " left in the chest";
            if (diamonds != 5 || more != 3 || linkedDiamonds != 8) return "link: saw " + diamonds + " diamonds, stored " + more + ", chest has " + linkedDiamonds;
            if (exported != 10) return "exporter delivered " + exported + " dirt";
            if (!saved || disk[0] <= 0) return "disk contents not in the world data";
            if (used <= 0 || n.upkeep() <= 0) return "core paid no upkeep";
            return null;
        };
    }

    private static ItemStack card(ServerLevel l, ItemStack... grid) {
        List<ItemStack> g = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (int i = 0; i < grid.length; i++) g.set(i, grid[i]);
        return Pattern.encode(l, g, true, new ItemStack(Content.RECIPE_CARD.get()));
    }

    private static CoreBE crafter(ServerLevel l, BlockPos o) {
        CoreBE core = network(l, o, 3);
        FabricatorBE fab = place(l, o.east(2).north(), Content.FABRICATOR.get(), FabricatorBE.class);
        fab.cards.setStackInSlot(0, card(l, new ItemStack(Items.OAK_LOG)));
        fab.cards.setStackInSlot(1, card(l, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS)));
        core.energy.setEnergy(CoreBE.CAPACITY);
        core.invalidateGrid();
        return core;
    }

    private static Supplier<String> autocraft(ServerLevel l, BlockPos o) {
        CoreBE core = crafter(l, o);
        DataGrid g = core.grid();
        if (g.patterns().size() != 2) return () -> "fabricator offers " + g.patterns().size() + " recipes, expected 2";
        g.insert(ItemKey.of(Items.OAK_LOG), 3, false);
        ItemKey stick = ItemKey.of(Items.STICK);
        CraftPlanner.Plan big = CraftPlanner.plan(g.snapshot(), g.patterns(), stick, 100);
        long missing = big.missing.getOrDefault(ItemKey.of(Items.OAK_LOG), 0L);
        CraftPlanner.Plan p = CraftPlanner.plan(g.snapshot(), g.patterns(), stick, 8);
        LOG.info("datastore:autocraft plan for 100 sticks ok={} missing {} logs; plan for 8 ok={} steps={} crafts={} uses {} logs",
                big.ok(), missing, p.ok(), p.steps.size(), p.totalCrafts(), p.used.get(ItemKey.of(Items.OAK_LOG)));
        if (big.ok() || missing != 10) return () -> "plan for 100 sticks should miss 10 logs, missing " + missing;
        if (!p.ok() || p.steps.size() != 2 || p.totalCrafts() != 3) return () -> "bad plan for 8 sticks: ok=" + p.ok() + " steps=" + p.steps.size();
        core.start(p);
        return () -> {
            DataGrid n = core.grid();
            long sticks = n.count(stick), logs = n.count(ItemKey.of(Items.OAK_LOG)), planks = n.count(ItemKey.of(Items.OAK_PLANKS));
            LOG.info("datastore:autocraft after the job: {} sticks, {} logs, {} planks, {} jobs left", sticks, logs, planks, core.jobs.size());
            if (sticks != 8 || logs != 2 || planks != 0 || !core.jobs.isEmpty())
                return "expected 8 sticks, 2 logs, 0 planks and no jobs; got " + sticks + ", " + logs + ", " + planks + ", " + core.jobs.size();
            return null;
        };
    }

    private static Supplier<String> substitutes(ServerLevel l, BlockPos o) {
        CoreBE core = crafter(l, o);
        DataGrid g = core.grid();
        g.insert(ItemKey.of(Items.BIRCH_PLANKS), 2, false);
        g.insert(ItemKey.of(Items.SPRUCE_PLANKS), 2, false);
        CraftPlanner.Plan p = CraftPlanner.plan(g.snapshot(), g.patterns(), ItemKey.of(Items.STICK), 8);
        if (!p.ok()) return () -> "substitute plan not ok, missing " + p.missing;
        core.start(p);
        return () -> {
            DataGrid n = core.grid();
            long sticks = n.count(ItemKey.of(Items.STICK));
            long rest = n.count(ItemKey.of(Items.BIRCH_PLANKS)) + n.count(ItemKey.of(Items.SPRUCE_PLANKS));
            LOG.info("datastore:substitutes {} sticks from birch and spruce planks ({} planks left)", sticks, rest);
            return sticks == 8 && rest == 0 ? null : "made " + sticks + " sticks, " + rest + " planks left";
        };
    }
}
