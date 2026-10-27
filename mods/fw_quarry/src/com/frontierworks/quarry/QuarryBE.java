package com.frontierworks.quarry;

import com.frontierworks.core.item.UpgradeItem;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Mines out a square area below itself, layer by layer down to bedrock. 9x9 by default (Range
 * Upgrades widen it), Speed Upgrades mine faster. Liquids are replaced with stone. Outputs into its
 * buffer and any inventory on top.
 */
public class QuarryBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 52)
            .grid(26, 17, 6, 3, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .button(0, 26, 74, 50, 18, "gui.fw_core.start")
            .info(82, 76).extra(3).height(186).build();
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

    private boolean running;
    private boolean done;
    private int cx, cy, cz;
    private boolean initialized;
    private int timer;

    public QuarryBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 200_000, 2_000, 0);
    }

    private int radius() {
        return 4 + 4 * Math.min(4, countUpgrades(UpgradeItem.Type.RANGE));
    }

    private void resetCursor() {
        int r = radius();
        cx = worldPosition.getX() - r;
        cz = worldPosition.getZ() - r;
        cy = worldPosition.getY() - 1;
        initialized = true;
        done = false;
    }

    private boolean advance() {
        int r = radius();
        cx++;
        if (cx > worldPosition.getX() + r) {
            cx = worldPosition.getX() - r;
            cz++;
            if (cz > worldPosition.getZ() + r) {
                cz = worldPosition.getZ() - r;
                cy--;
                if (level == null || cy <= level.getMinBuildHeight()) {
                    done = true;
                    running = false;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        pushOutputs();
        if (!running || done) {
            setLit(false);
            return;
        }
        if (!initialized) resetCursor();
        int interval = Math.max(1, 8 - 2 * countUpgrades(UpgradeItem.Type.SPEED));
        if (++timer < interval) return;
        timer = 0;
        // skip up to 64 cheap blocks (air, unbreakable, tile entities) per operation
        for (int skip = 0; skip < 64; skip++) {
            BlockPos p = new BlockPos(cx, cy, cz);
            if (!level.isLoaded(p)) return;
            BlockState st = level.getBlockState(p);
            if (st.isAir() || st.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null || p.equals(worldPosition)) {
                if (!advance()) return;
                continue;
            }
            if (!st.getFluidState().isEmpty()) {
                level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
                if (!advance()) return;
                continue;
            }
            int cost = (int) (60 + st.getDestroySpeed(level, p) * 40);
            cost = (int) Math.round(cost * energyMultiplier());
            if (energy.getEnergyStored() < cost) return;
            List<ItemStack> drops = Block.getDrops(st, (ServerLevel) level, p, null, null, TOOL);
            if (!outputsFit(drops)) return;
            energy.consume(cost);
            for (ItemStack d : drops) insertOutput(d, false);
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            level.levelEvent(2001, p, Block.getId(st));
            advance();
            setLit(true);
            setChanged();
            return;
        }
    }

    private void pushOutputs() {
        if (level.getGameTime() % 10 != 0) return;
        BlockEntity above = level.getBlockEntity(worldPosition.above());
        if (above == null) return;
        IItemHandler h = above.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
        if (h == null) return;
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role != MachineLayout.Role.OUTPUT) continue;
            ItemStack st = items.getStackInSlot(i);
            if (st.isEmpty()) continue;
            ItemStack rem = ItemHandlerHelper.insertItem(h, st.copy(), false);
            items.setStackInSlot(i, rem);
        }
    }

    @Override
    public void onButton(int id, Player player) {
        if (id == 0) {
            if (done) resetCursor();
            running = !running;
            setChanged();
        }
    }

    @Override
    protected void applyUpgrades() {
        super.applyUpgrades();
        initialized = false;
    }

    @Override
    public int getExtra(int i) {
        switch (i) {
            case 0: return running ? 1 : 0;
            case 1: return cy;
            default: return done ? 1 : 0;
        }
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        return Component.translatable(menu.extra(0) == 1 ? "gui.fw_core.stop" : "gui.fw_core.start");
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        if (menu.extra(2) == 1) lines.add(Component.translatable("gui.fw_quarry.done"));
        else lines.add(Component.translatable("gui.fw_quarry.layer", menu.extra(1)));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putBoolean("Running", running);
        tag.putBoolean("Done", done);
        tag.putBoolean("Init", initialized);
        tag.putInt("CX", cx);
        tag.putInt("CY", cy);
        tag.putInt("CZ", cz);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        running = tag.getBoolean("Running");
        done = tag.getBoolean("Done");
        initialized = tag.getBoolean("Init");
        cx = tag.getInt("CX");
        cy = tag.getInt("CY");
        cz = tag.getInt("CZ");
    }
}
