package com.frontierworks.teleporters;

import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import com.frontierworks.core.util.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Teleport pad: link it to another pad with a Coordinate Card. A player who crouches on it for one
 * second is teleported to the linked pad, paying energy by distance (much more across dimensions).
 */
public class TeleportPadBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder().energy(8, 17, 52).info(26, 20).extra(2).build();
    @Nullable
    private BlockPos target;
    @Nullable
    private ResourceLocation targetDim;
    private final Map<UUID, Integer> charge = new HashMap<>();

    public TeleportPadBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 200_000, 5_000, 0);
    }

    public void link(BlockPos pos, ResourceLocation dim) {
        target = pos;
        targetDim = dim;
        setChanged();
    }

    private int cost() {
        if (target == null || level == null) return 0;
        if (targetDim != null && !targetDim.equals(level.dimension().location())) return 100_000;
        return 1_000 + (int) Math.sqrt(target.distSqr(worldPosition)) * 20;
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        dischargeBatteries();
        if (target == null || targetDim == null || level.getGameTime() % 2 != 0) return;
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(worldPosition.above()).inflate(0.1, 0.5, 0.1), Player::isShiftKeyDown);
        charge.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));
        for (Player p : players) {
            int c = charge.merge(p.getUUID(), 2, Integer::sum);
            if (level instanceof ServerLevel) {
                ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.6, 0.3, 0.2);
            }
            if (c >= 20) {
                charge.remove(p.getUUID());
                int cost = cost();
                if (energy.getEnergyStored() < cost) {
                    p.displayClientMessage(Component.translatable("msg.fw_teleporters.no_power", cost), true);
                    continue;
                }
                ServerLevel dest = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDim));
                if (dest == null) continue;
                energy.consume(cost);
                level.playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1f);
                TeleportUtil.teleport(p, dest, target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5, p.getYRot(), p.getXRot());
                dest.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1.2f);
            }
        }
    }

    @Override
    public int getExtra(int i) {
        return i == 0 ? (target != null ? 1 : 0) : cost();
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        if (menu.extra(0) == 0) {
            lines.add(Component.translatable("gui.fw_teleporters.unlinked"));
        } else {
            lines.add(Component.translatable("gui.fw_teleporters.linked"));
            lines.add(Component.translatable("gui.fw_teleporters.cost", menu.extra(1)));
        }
        lines.add(Component.translatable("gui.fw_teleporters.howto"));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        if (target != null && targetDim != null) {
            tag.putLong("Target", target.asLong());
            tag.putString("TargetDim", targetDim.toString());
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        if (tag.contains("Target")) {
            target = BlockPos.of(tag.getLong("Target"));
            targetDim = new ResourceLocation(tag.getString("TargetDim"));
        }
    }
}
