package com.frontierworks.mobgrinder;

import com.frontierworks.core.item.UpgradeItem;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Mob Grinder: damages mobs in a cube above it as if a player hit them (so they drop rare loot and XP),
 * using the weapon in its weapon slot. Collects drops and stores the experience.
 * Never harms players, villagers, golems, tamed pets, named mobs or bosses.
 */
public class MobGrinderBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(8, 17, 52)
            .slot(26, 17, MachineLayout.Role.INPUT)
            .grid(62, 17, 4, 3, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE)
            .button(0, 26, 72, 60, 16, "gui.fw_mobgrinder.take_xp").info(92, 76).extra(1).height(186).build();
    private int xp;
    private int timer;

    public MobGrinderBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 100_000, 1_000, 0);
    }

    @Override
    public int getSlotLimit(int slot) {
        return layout.slots.get(slot).role == MachineLayout.Role.INPUT ? 1 : super.getSlotLimit(slot);
    }

    private AABB area() {
        int r = 2 + 2 * countUpgrades(UpgradeItem.Type.RANGE);
        return new AABB(worldPosition.offset(-r, 1, -r), worldPosition.offset(r + 1, 4, r + 1));
    }

    private static boolean valid(LivingEntity e) {
        if (!(e instanceof Mob) || !e.isAlive() || e.hasCustomName()) return false;
        if (e instanceof AbstractVillager || e instanceof IronGolem || e instanceof WitherBoss || e instanceof EnderDragon) return false;
        if (e instanceof TamableAnimal && ((TamableAnimal) e).isTame()) return false;
        return !e.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        int interval = Math.max(5, 20 - 5 * countUpgrades(UpgradeItem.Type.SPEED));
        if (++timer < interval) return;
        timer = 0;
        AABB box = area();
        boolean hit = false;
        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, box, MobGrinderBE::valid);
        if (!mobs.isEmpty() && energy.getEnergyStored() >= 200) {
            FakePlayer fp = FakePlayerFactory.getMinecraft((ServerLevel) level);
            fp.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
            ItemStack weapon = items.getStackInSlot(0);
            fp.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
            float damage = 6.0f + (weapon.isEmpty() ? 0 : 4.0f);
            for (LivingEntity e : mobs) {
                if (energy.getEnergyStored() < 200) break;
                e.hurt(level.damageSources().playerAttack(fp), damage);
                energy.consume(200);
                hit = true;
            }
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        AABB pickup = box.inflate(1);
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, pickup, e -> e.isAlive())) {
            ItemStack rem = insertOutput(ie.getItem(), false);
            if (rem.isEmpty()) ie.discard();
            else ie.setItem(rem);
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, pickup, e -> e.isAlive())) {
            xp += orb.getValue();
            orb.discard();
        }
        setLit(hit);
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
        lines.add(Component.translatable("gui.fw_mobgrinder.xp", menu.extra(0)));
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
