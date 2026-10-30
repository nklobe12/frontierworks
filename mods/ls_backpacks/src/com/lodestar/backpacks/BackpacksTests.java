package com.lodestar.backpacks;

import com.lodestar.core.test.SelfTest;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/** Self-test: contents follow a backpack through being placed and picked up; magnet and smelting upgrades work. */
final class BackpacksTests {
    private static final Logger LOG = LogUtils.getLogger();

    private BackpacksTests() {}

    static void register() {
        SelfTest.scenario("backpacks:place_and_keep", 20, BackpacksTests::placeAndKeep);
        SelfTest.scenario("backpacks:upgrades", 220, BackpacksTests::upgrades);
    }

    private static Supplier<String> placeAndKeep(ServerLevel l, BlockPos o) {
        l.setBlock(o.below(), Blocks.STONE.defaultBlockState(), 3);
        ItemStack pack = new ItemStack(Content.IRON_BACKPACK.get());
        UUID id = BackpackItem.id(pack);
        Contents c = BackpackStore.get(l.getServer()).contents(id, BackpackTier.IRON.slots());
        c.items.insertItem(0, new ItemStack(Items.DIAMOND, 7), false);
        l.setBlock(o, Content.PLACED_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.TIER, BackpackBlock.TierProp.IRON), 3);
        BackpackBE be = (BackpackBE) l.getBlockEntity(o);
        be.fromItem(pack);
        IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
        int seen = h == null ? -1 : h.getStackInSlot(0).getCount();
        ItemStack back = be.toItem();
        UUID backId = BackpackItem.peekId(back);
        l.removeBlock(o, false);
        return () -> {
            LOG.info("backpacks:place_and_keep block sees {} diamonds, id kept={}", seen, id.equals(backId));
            if (seen != 7) return "placed backpack exposes " + seen + " diamonds instead of 7";
            if (!id.equals(backId)) return "picked-up backpack lost its id";
            if (!(back.getItem() instanceof BackpackItem bi) || bi.tier != BackpackTier.IRON) return "picked-up backpack has the wrong tier";
            return null;
        };
    }

    private static Supplier<String> upgrades(ServerLevel l, BlockPos o) {
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) l.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
        FakePlayer fp = FakePlayerFactory.getMinecraft(l);
        fp.setPos(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
        ItemEntity loose = new ItemEntity(l, o.getX() + 4.5, o.getY() + 0.5, o.getZ() + 0.5, new ItemStack(Items.APPLE, 3));
        loose.setNoPickUpDelay();
        loose.setNoGravity(true);
        loose.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        l.addFreshEntity(loose);
        // entities in freshly force-loaded chunks are only visible to searches a few ticks later, so the magnet runs in the check
        // smelting runs in a placed backpack too
        ItemStack pack = new ItemStack(Content.LEATHER_BACKPACK.get());
        UUID id = BackpackItem.id(pack);
        Contents c = BackpackStore.get(l.getServer()).contents(id, BackpackTier.LEATHER.slots());
        c.upgrades.insertItem(0, new ItemStack(Content.SMELTING_UPGRADE.get()), false);
        c.smelt.insertItem(0, new ItemStack(Items.RAW_IRON, 1), false);
        c.smelt.insertItem(1, new ItemStack(Items.COAL, 1), false);
        BlockPos bp = o.east(2);
        l.setBlock(bp, Content.PLACED_BACKPACK.get().defaultBlockState(), 3);
        ((BackpackBE) l.getBlockEntity(bp)).fromItem(pack);
        // refill from a backpack into a hotbar stack
        ItemStack hot = new ItemStack(Items.COBBLESTONE, 10);
        fp.getInventory().setItem(0, hot);
        c.items.insertItem(3, new ItemStack(Items.COBBLESTONE, 64), false);
        if (l.getServer() != null) Upgrades.refill(fp, c);
        int hotCount = fp.getInventory().getItem(0).getCount();
        fp.getInventory().setItem(0, ItemStack.EMPTY);
        return () -> {
            fp.setPos(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
            Upgrades.magnet(fp);
            double dist = loose.position().distanceTo(fp.position());
            int iron = c.smelt.getStackInSlot(2).is(Items.IRON_INGOT) ? c.smelt.getStackInSlot(2).getCount() : 0;
            LOG.info("backpacks:upgrades magnet moved item to {} blocks away, smelted {} iron, hotbar refilled to {}", dist, iron, hotCount);
            loose.discard();
            l.removeBlock(bp, false);
            if (dist > 1.0) return "magnet left the item " + dist + " blocks away";
            if (iron != 1) return "smelting upgrade produced " + iron + " iron ingots";
            if (hotCount != 64) return "refill left the hotbar stack at " + hotCount;
            return null;
        };
    }
}
