package com.lodestar.ducts.transit;

import com.lodestar.core.net.CoreNet;
import com.lodestar.ducts.DuctsSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Route finding through viaducts and the server side of picking a destination. */
public final class Transit {
    public static final int MAX_TUBES = 8192;

    private Transit() {}

    public record Route(BlockPos station, String name, List<BlockPos> path) {}

    /** Every station reachable from {@code origin} through viaducts, nearest first; path runs origin → station. */
    public static List<Route> routes(Level level, BlockPos origin) {
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        List<Route> out = new ArrayList<>();
        parent.put(origin, origin);
        q.add(origin);
        while (!q.isEmpty() && parent.size() < MAX_TUBES) {
            BlockPos p = q.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (parent.containsKey(n) || !level.isLoaded(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.getBlock() instanceof ViaductBlock) {
                    parent.put(n, p);
                    q.add(n);
                } else if (s.getBlock() instanceof StationBlock && !p.equals(origin)) {
                    parent.put(n, p);
                    List<BlockPos> path = new ArrayList<>();
                    for (BlockPos c = n; !c.equals(origin); c = parent.get(c)) path.add(0, c);
                    path.add(0, origin);
                    String name = level.getBlockEntity(n) instanceof StationBE be ? be.name() : "?";
                    out.add(new Route(n, name, path));
                }
            }
        }
        return out;
    }

    public static void openDestinations(ServerPlayer player, BlockPos origin) {
        List<Route> rs = routes(player.level(), origin);
        if (rs.isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.ls_ducts.no_stations"), true);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
        String here = player.level().getBlockEntity(origin) instanceof StationBE be ? be.name() : "";
        tag.putString("Here", here);
        ListTag l = new ListTag();
        for (Route r : rs) {
            CompoundTag t = new CompoundTag();
            t.putString("Name", r.name());
            t.put("Pos", NbtUtils.writeBlockPos(r.station()));
            t.putInt("Len", r.path().size() - 1);
            l.add(t);
        }
        tag.put("List", l);
        CoreNet.toPlayer(player, "ls_ducts:stations", tag);
    }

    /** Client picked a destination. */
    public static void travel(ServerPlayer player, CompoundTag tag) {
        BlockPos origin = NbtUtils.readBlockPos(tag.getCompound("Origin"));
        BlockPos dest = NbtUtils.readBlockPos(tag.getCompound("Dest"));
        Level level = player.level();
        if (player.isPassenger() || player.distanceToSqr(Vec3.atCenterOf(origin)) > 36) return;
        if (!(level.getBlockState(origin).getBlock() instanceof StationBlock)) return;
        for (Route r : routes(level, origin)) {
            if (!r.station().equals(dest)) continue;
            List<Vec3> pts = new ArrayList<>();
            for (BlockPos p : r.path()) pts.add(Vec3.atCenterOf(p));
            TransitPod pod = new TransitPod(DuctsSetup.TRANSIT_POD.get(), level);
            pod.start(pts, dest);
            level.addFreshEntity(pod);
            player.startRiding(pod, true);
            level.playSound(null, origin, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
            return;
        }
    }
}
