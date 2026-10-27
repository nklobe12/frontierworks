package com.frontierworks.waystones;

import com.frontierworks.core.util.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Slot-less menu carrying the player's waystone list; button i teleports to waystone i. */
public class WaystoneMenu extends AbstractContainerMenu {
    public final BlockPos origin;
    public final List<WaystoneData.Entry> entries;

    public WaystoneMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), read(buf));
    }

    public WaystoneMenu(int id, Inventory inv, BlockPos origin, List<WaystoneData.Entry> entries) {
        super(Content.WAYSTONE_MENU.get(), id);
        this.origin = origin;
        this.entries = entries;
    }

    public static void write(FriendlyByteBuf buf, BlockPos origin, List<WaystoneData.Entry> list) {
        buf.writeBlockPos(origin);
        buf.writeVarInt(list.size());
        for (WaystoneData.Entry e : list) {
            buf.writeBlockPos(e.pos);
            buf.writeUtf(e.dim);
            buf.writeUtf(e.name);
        }
    }

    private static List<WaystoneData.Entry> read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<WaystoneData.Entry> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new WaystoneData.Entry(buf.readBlockPos(), buf.readUtf(), buf.readUtf()));
        return out;
    }

    public static int cost(Player p, BlockPos from, WaystoneData.Entry to) {
        if (p.getAbilities().instabuild) return 0;
        if (!to.dim.equals(p.level().dimension().location().toString())) return 5;
        return Math.max(1, (int) Math.ceil(Math.sqrt(from.distSqr(to.pos)) / 1000.0));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= entries.size() || !(player instanceof ServerPlayer)) return false;
        WaystoneData.Entry e = entries.get(id);
        if (e.pos.equals(origin)) return false;
        ServerPlayer sp = (ServerPlayer) player;
        ServerLevel dest = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(e.dim)));
        if (dest == null) return false;
        if (!(dest.getBlockEntity(e.pos) instanceof WaystoneBE)) {
            dest.getChunk(e.pos);
            if (!(dest.getBlockEntity(e.pos) instanceof WaystoneBE)) {
                WaystoneData.remove(player, e.pos, e.dim);
                player.displayClientMessage(Component.translatable("msg.fw_waystones.gone", e.name), false);
                sp.closeContainer();
                return true;
            }
        }
        int cost = cost(player, origin, e);
        if (player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("msg.fw_waystones.cost", cost), true);
            return false;
        }
        player.giveExperienceLevels(-cost);
        BlockPos land = e.pos.above();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = e.pos.relative(d);
            if (dest.isEmptyBlock(p) && dest.isEmptyBlock(p.above()) && !dest.isEmptyBlock(p.below())) {
                land = p;
                break;
            }
        }
        sp.closeContainer();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        TeleportUtil.teleport(sp, dest, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        dest.playSound(null, land, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5) < 64;
    }
}
