package com.frontierworks.farming;

import com.frontierworks.core.item.UpgradeItem;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Crop Harvester: scans the area around itself (same height) for mature crops, harvests and replants
 * them, cuts sugar cane above the base, harvests melons and pumpkins, and spreads bone meal/fertilizer.
 */
public class HarvesterBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(8, 17, 52)
            .slot(26, 17, MachineLayout.Role.INPUT)
            .grid(62, 17, 4, 3, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .info(26, 40).build();
    private static final ItemStack HOE = new ItemStack(Items.IRON_HOE);
    private int index;
    private int timer;

    public HarvesterBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 40_000, 400, 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.INPUT) {
            return stack.getItem() instanceof BoneMealItem || stack.is(com.frontierworks.core.util.FW.item("fw_chemistry:fertilizer"));
        }
        return super.isItemValid(slot, stack);
    }

    private int radius() {
        return 4 + 2 * countUpgrades(UpgradeItem.Type.RANGE);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        int interval = Math.max(1, 6 - 2 * countUpgrades(UpgradeItem.Type.SPEED));
        if (++timer < interval) return;
        timer = 0;
        int r = radius();
        int side = 2 * r + 1;
        boolean worked = false;
        for (int n = 0; n < 4; n++) {
            index = (index + 1) % (side * side);
            BlockPos p = worldPosition.offset(index % side - r, 0, index / side - r);
            if (p.equals(worldPosition)) continue;
            if (energy.getEnergyStored() < 50) break;
            if (tryHarvest(p)) {
                energy.consume(50);
                worked = true;
                break;
            }
            if (fertilize(p)) {
                energy.consume(20);
            }
        }
        setLit(worked);
    }

    private boolean tryHarvest(BlockPos p) {
        BlockState st = level.getBlockState(p);
        Block b = st.getBlock();
        if (b instanceof CropBlock && ((CropBlock) b).isMaxAge(st)) {
            List<ItemStack> drops = Block.getDrops(st, (ServerLevel) level, p, null, null, HOE);
            ItemStack seed = b.getCloneItemStack(level, p, st);
            boolean replant = removeOne(drops, seed);
            if (!outputsFit(drops)) return false;
            for (ItemStack d : drops) insertOutput(d, false);
            level.setBlock(p, replant ? ((CropBlock) b).getStateForAge(0) : Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        if (b instanceof NetherWartBlock && st.getValue(NetherWartBlock.AGE) >= 3) {
            List<ItemStack> drops = Block.getDrops(st, (ServerLevel) level, p, null, null, HOE);
            removeOne(drops, new ItemStack(Items.NETHER_WART));
            if (!outputsFit(drops)) return false;
            for (ItemStack d : drops) insertOutput(d, false);
            level.setBlock(p, st.setValue(NetherWartBlock.AGE, 0), 3);
            return true;
        }
        if (b instanceof StemGrownBlock) {
            List<ItemStack> drops = Block.getDrops(st, (ServerLevel) level, p, null, null, HOE);
            if (!outputsFit(drops)) return false;
            for (ItemStack d : drops) insertOutput(d, false);
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        BlockState above = level.getBlockState(p.above());
        if (b instanceof SugarCaneBlock && above.getBlock() instanceof SugarCaneBlock) {
            BlockPos top = p.above();
            int n = 0;
            while (level.getBlockState(top.above()).getBlock() instanceof SugarCaneBlock) top = top.above();
            for (BlockPos q = top; q.getY() > p.getY(); q = q.below()) {
                level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                n++;
            }
            insertOutput(new ItemStack(Items.SUGAR_CANE, n), false);
            return true;
        }
        return false;
    }

    private boolean fertilize(BlockPos p) {
        ItemStack fert = items.getStackInSlot(0);
        if (fert.isEmpty() || level.random.nextInt(4) != 0) return false;
        BlockState st = level.getBlockState(p);
        if (!(st.getBlock() instanceof CropBlock) || ((CropBlock) st.getBlock()).isMaxAge(st)) return false;
        ItemStack copy = new ItemStack(Items.BONE_MEAL);
        if (BoneMealItem.applyBonemeal(copy, level, p, FakePlayerFactory.getMinecraft((ServerLevel) level))) {
            if (!(fert.getItem() instanceof BoneMealItem) && level.random.nextBoolean()) {
                BoneMealItem.applyBonemeal(copy, level, p, FakePlayerFactory.getMinecraft((ServerLevel) level));
            }
            items.extractItem(0, 1, false);
            level.levelEvent(1505, p, 0);
            return true;
        }
        return false;
    }

    private static boolean removeOne(List<ItemStack> drops, ItemStack seed) {
        if (seed.isEmpty()) return false;
        for (ItemStack d : drops) {
            if (ItemStack.isSameItem(d, seed) && d.getCount() > 0) {
                d.shrink(1);
                drops.removeIf(ItemStack::isEmpty);
                return true;
            }
        }
        return false;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.fw_farming.fert"));
    }
}
