package com.frontierworks.vacuum;

import com.frontierworks.core.item.UpgradeItem;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

/** Sucks up items (and experience) in a radius, stores them, and pushes them into an inventory below. */
public class VacuumHopperBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder().grid(8, 18, 9, 2, MachineLayout.Role.STORAGE)
            .slot(152, 58, MachineLayout.Role.UPGRADE).button(0, 8, 58, 80, 18, "gui.fw_vacuum.take_xp").info(92, 62).extra(1).height(176).build();
    private int xp;

    public VacuumHopperBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 0, 0, 0);
    }

    @Override
    public void serverTick() {
        if (level == null || level.getGameTime() % 4 != 0) return;
        int r = 4 + 3 * countUpgrades(UpgradeItem.Type.RANGE);
        AABB box = new AABB(worldPosition).inflate(r);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.hasPickUpDelay());
        for (ItemEntity ie : items) {
            ItemStack rem = ie.getItem().copy();
            for (int i = 0; i < 18 && !rem.isEmpty(); i++) rem = this.items.insertItem(i, rem, false);
            if (rem.isEmpty()) ie.discard();
            else ie.setItem(rem);
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box, e -> e.isAlive())) {
            xp += orb.getValue();
            orb.discard();
        }
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below != null) {
            IItemHandler h = below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
            if (h != null) {
                for (int i = 0; i < 18; i++) {
                    ItemStack st = this.items.extractItem(i, 16, true);
                    if (st.isEmpty()) continue;
                    ItemStack rem = ItemHandlerHelper.insertItem(h, st, false);
                    this.items.extractItem(i, st.getCount() - rem.getCount(), false);
                    break;
                }
            }
        }
        setChanged();
    }

    @Override
    public void onButton(int id, Player player) {
        if (id == 0 && xp > 0) {
            player.giveExperiencePoints(xp);
            xp = 0;
            setChanged();
        }
    }

    @Override
    public int getExtra(int i) {
        return xp;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.fw_vacuum.xp", menu.extra(0)));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("XP", xp);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        xp = tag.getInt("XP");
    }
}
