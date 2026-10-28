package com.lodestar.quarry;

import com.lodestar.core.test.SelfTest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.function.Supplier;

/** Functional checks: a frame quarry digging a marked 3x3, a laser quarry with an ore sieve, and land restoration. */
final class QuarryTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private QuarryTests() {}

    static void register() {
        SelfTest.scenario("quarry:frame", 300, QuarryTests::frame);
        SelfTest.scenario("quarry:laser", 200, QuarryTests::laser);
        SelfTest.scenario("quarry:restore", 360, QuarryTests::restore);
    }

    private static void box(ServerLevel l, BlockPos a, BlockPos b, BlockState st) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) l.setBlock(p, st, 2);
    }

    /** Clears the site and lays three ground layers: top (y-1), middle (y-2), bottom (y-3). */
    private static void site(ServerLevel l, BlockPos o, BlockState top, BlockState mid, BlockState bottom) {
        box(l, o.offset(-3, 0, -3), o.offset(9, 8, 9), Blocks.AIR.defaultBlockState());
        box(l, o.offset(-3, -1, -3), o.offset(9, -1, 9), top);
        box(l, o.offset(-3, -2, -3), o.offset(9, -2, 9), mid);
        box(l, o.offset(-3, -3, -3), o.offset(9, -3, 9), bottom);
    }

    private static int count(ServerLevel l, BlockPos p, Item item) {
        var be = l.getBlockEntity(p);
        if (be == null) return 0;
        IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        int n = 0;
        if (h != null) for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).is(item)) n += h.getStackInSlot(i).getCount();
        return n;
    }

    private static int countBlocks(ServerLevel l, BlockPos a, BlockPos b, Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(a, b)) if (l.getBlockState(p).is(block)) n++;
        return n;
    }

    /** Markers on the corners of a 5x5 frame (interior 3x3 at +2..+4), quarry just west of the corner marker. */
    private static QuarryBE markedQuarry(ServerLevel l, BlockPos o) {
        BlockState marker = Content.SURVEY_MARKER.get().defaultBlockState();
        l.setBlock(o.offset(1, 0, 1), marker, 3);
        l.setBlock(o.offset(5, 0, 1), marker, 3);
        l.setBlock(o.offset(1, 0, 5), marker, 3);
        ((MarkerBE) l.getBlockEntity(o.offset(1, 0, 1))).link(null, true);
        l.setBlock(o.offset(0, 0, 1), Content.QUARRY.get().defaultBlockState(), 3);
        QuarryBE q = (QuarryBE) l.getBlockEntity(o.offset(0, 0, 1));
        q.getEnergy().setEnergy(80_000);
        return q;
    }

    private static Supplier<String> frame(ServerLevel l, BlockPos o) {
        site(l, o, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        l.setBlock(o.offset(3, -1, 3), Blocks.IRON_ORE.defaultBlockState(), 2);
        QuarryBE q = markedQuarry(l, o);
        BlockPos chest = o.offset(0, 1, 1);
        l.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        return () -> {
            int air = countBlocks(l, o.offset(2, -1, 2), o.offset(4, -1, 4), Blocks.AIR);
            int frames = countBlocks(l, o.offset(1, 0, 1), o.offset(5, 4, 5), Content.QUARRY_FRAME.get());
            BlockPos qp = o.offset(0, 0, 1);
            int cobble = count(l, qp, Items.COBBLESTONE) + count(l, chest, Items.COBBLESTONE);
            int iron = count(l, qp, Items.RAW_IRON) + count(l, chest, Items.RAW_IRON);
            int markers = count(l, qp, Content.SURVEY_MARKER.get().asItem()) + count(l, chest, Content.SURVEY_MARKER.get().asItem());
            LOG.info("quarry:frame phase={} mined={} frame blocks={} cleared={}/9 cobblestone={} raw iron={} markers returned={}",
                    q.phase(), q.mined(), frames, air, cobble, iron, markers);
            if (frames < 44) return "frame has " + frames + " of 44 blocks";
            if (air < 9) return "only " + air + " of 9 blocks of the first stone layer dug";
            if (iron < 1 || cobble < 8) return "outputs: " + cobble + " cobblestone, " + iron + " raw iron";
            return markers == 3 ? null : "expected the 3 markers back, got " + markers;
        };
    }

    private static Supplier<String> laser(ServerLevel l, BlockPos o) {
        site(l, o, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        BlockPos c = o.offset(3, 0, 3);
        l.setBlock(c.offset(-1, -1, 0), Blocks.IRON_ORE.defaultBlockState(), 2);
        l.setBlock(c, Content.LASER_MAST.get().defaultBlockState(), 3);
        l.setBlock(c.above(), Content.LASER_MAST.get().defaultBlockState(), 3);
        BlockPos qp = c.above(2);
        l.setBlock(qp, Content.LASER_QUARRY.get().defaultBlockState(), 3);
        LaserQuarryBE q = (LaserQuarryBE) l.getBlockEntity(qp);
        q.setRadius(1);
        q.getItems().setStackInSlot(AbstractQuarryBE.S_SIEVE, new ItemStack(Content.ORE_SIEVE.get()));
        q.getEnergy().setEnergy(200_000);
        return () -> {
            int air = countBlocks(l, c.offset(-1, -1, -1), c.offset(1, -1, 1), Blocks.AIR);
            int iron = count(l, qp, Items.RAW_IRON), cobble = count(l, qp, Items.COBBLESTONE);
            boolean tower = l.getBlockState(c.below()).is(Blocks.STONE);
            LOG.info("quarry:laser phase={} mined={} first stone ring cleared={}/8 raw iron={} cobblestone={} (voided) column under tower kept={}",
                    q.phase(), q.mined(), air, iron, cobble, tower);
            if (air < 8) return "only " + air + " of 8 blocks around the tower dug";
            if (!tower) return "dug the column under the tower";
            return iron >= 1 && cobble == 0 ? null : "sieve failed: raw iron " + iron + ", cobblestone " + cobble;
        };
    }

    private static Supplier<String> restore(ServerLevel l, BlockPos o) {
        site(l, o, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState());
        QuarryBE q = markedQuarry(l, o);
        q.setRestore(true);
        q.setFloorLimit(o.getY() - 2);
        return () -> {
            int grass = countBlocks(l, o.offset(2, -1, 2), o.offset(4, -1, 4), Blocks.GRASS_BLOCK);
            int dirt = countBlocks(l, o.offset(2, -2, 2), o.offset(4, -2, 4), Blocks.DIRT);
            int frames = countBlocks(l, o.offset(1, 0, 1), o.offset(5, 4, 5), Content.QUARRY_FRAME.get());
            LOG.info("quarry:restore phase={} mined={} restored grass={}/9 dirt={}/9 frame blocks left={}", q.phase(), q.mined(), grass, dirt, frames);
            if (q.mined() < 18) return "dug only " + q.mined() + " blocks";
            if (q.phase() != AbstractQuarryBE.Phase.DONE) return "still in phase " + q.phase();
            if (grass < 9 || dirt < 9) return "restored " + grass + " grass and " + dirt + " dirt of 9 each";
            return frames == 0 ? null : frames + " frame blocks left standing";
        };
    }
}
