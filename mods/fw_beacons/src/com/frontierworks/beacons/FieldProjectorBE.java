package com.frontierworks.beacons;

import com.frontierworks.core.item.UpgradeItem;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Powered effect field: applies a chosen beneficial effect to every player in range, paid for in FE. */
public class FieldProjectorBE extends MachineBlockEntity {
    private static final MobEffect[] EFFECTS = {MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE,
            MobEffects.NIGHT_VISION, MobEffects.JUMP, MobEffects.DAMAGE_BOOST, MobEffects.WATER_BREATHING, MobEffects.FIRE_RESISTANCE, MobEffects.SATURATION};
    private static final int[] COST = {1500, 1500, 4000, 3000, 800, 800, 3000, 800, 1500, 6000};
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(8, 17, 52)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE)
            .button(0, 26, 54, 70, 18, "gui.fw_beacons.next").button(1, 98, 54, 50, 18, "gui.fw_core.on")
            .info(26, 20).extra(3).build();
    private int effect;
    private boolean enabled = true;
    private int timer;

    public FieldProjectorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 500_000, 10_000, 0);
    }

    private int range() {
        return 16 + 16 * countUpgrades(UpgradeItem.Type.RANGE);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        if (!enabled || ++timer < 80) return;
        timer = 0;
        int amp = Math.min(2, countUpgrades(UpgradeItem.Type.SPEED) / 2);
        int cost = (int) Math.round(COST[effect] * (1 + amp) * energyMultiplier());
        if (energy.getEnergyStored() < cost) {
            setLit(false);
            return;
        }
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(range()));
        if (players.isEmpty()) {
            setLit(false);
            return;
        }
        energy.consume(cost);
        for (Player p : players) {
            p.addEffect(new MobEffectInstance(EFFECTS[effect], effect == 4 ? 400 : 200, effect == 9 ? 0 : amp, true, true));
        }
        setLit(true);
    }

    @Override
    public void onButton(int id, Player player) {
        if (id == 0) effect = (effect + 1) % EFFECTS.length;
        if (id == 1) enabled = !enabled;
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        return i == 0 ? effect : i == 1 ? (enabled ? 1 : 0) : range();
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        if (id == 0) return Component.translatable("gui.fw_beacons.next");
        return Component.translatable(menu.extra(1) == 1 ? "gui.fw_core.on" : "gui.fw_core.off");
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int e = Math.max(0, Math.min(EFFECTS.length - 1, menu.extra(0)));
        lines.add(EFFECTS[e].getDisplayName());
        lines.add(Component.translatable("gui.fw_beacons.range", menu.extra(2)));
        lines.add(Component.translatable("gui.fw_beacons.cost", COST[e]));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Effect", effect);
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        effect = Math.max(0, Math.min(EFFECTS.length - 1, tag.getInt("Effect")));
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}
