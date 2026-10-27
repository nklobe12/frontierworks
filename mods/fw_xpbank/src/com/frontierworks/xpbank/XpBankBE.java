package com.frontierworks.xpbank;

import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Stores experience points safely. Buttons deposit/withdraw one level or everything. */
public class XpBankBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .button(0, 10, 40, 76, 18, "gui.fw_xpbank.deposit1").button(1, 90, 40, 76, 18, "gui.fw_xpbank.deposit_all")
            .button(2, 10, 60, 76, 18, "gui.fw_xpbank.withdraw1").button(3, 90, 60, 76, 18, "gui.fw_xpbank.withdraw_all")
            .info(10, 20).extra(1).build();
    private int points;

    public XpBankBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 0, 0, 0);
    }

    @Override
    public void serverTick() {}

    public static int pointsForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    public static int totalPoints(Player p) {
        return pointsForLevel(p.experienceLevel) + Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
    }

    @Override
    public void onButton(int id, Player p) {
        int have = totalPoints(p);
        switch (id) {
            case 0: {
                if (p.experienceLevel <= 0) {
                    int t = have;
                    p.giveExperiencePoints(-t);
                    points += t;
                    break;
                }
                int t = have - pointsForLevel(p.experienceLevel - 1);
                p.giveExperiencePoints(-t);
                points += t;
                break;
            }
            case 1:
                p.giveExperiencePoints(-have);
                p.experienceLevel = 0;
                p.experienceProgress = 0;
                p.totalExperience = 0;
                points += have;
                break;
            case 2: {
                int need = pointsForLevel(p.experienceLevel + 1) - have;
                int give = Math.min(points, Math.max(1, need));
                p.giveExperiencePoints(give);
                points -= give;
                break;
            }
            default:
                p.giveExperiencePoints(points);
                points = 0;
                break;
        }
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        return points;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int pts = menu.extra(0);
        int lvl = 0;
        while (pointsForLevel(lvl + 1) <= pts) lvl++;
        lines.add(Component.translatable("gui.fw_xpbank.stored", pts, lvl));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("XP", points);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        points = tag.getInt("XP");
    }
}
