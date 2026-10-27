package com.lodestar.ducts.transit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Carries a player through viaducts along a fixed list of points. Server and client run the same movement from the
 * same starting state, so the ride is smooth without position packets.
 */
public class TransitPod extends Entity implements IEntityAdditionalSpawnData {
    public static final double SPEED = 0.7;
    private final List<Vec3> points = new ArrayList<>();
    private int seg;
    private double along;
    private BlockPos dest = BlockPos.ZERO;
    public boolean arrived;

    public TransitPod(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void start(List<Vec3> pts, BlockPos dest) {
        points.clear();
        points.addAll(pts);
        seg = 0;
        along = 0;
        this.dest = dest;
        place();
        setOldPosAndRot();
    }

    private Vec3 current() {
        if (points.isEmpty()) return position();
        if (seg >= points.size() - 1) return points.get(points.size() - 1);
        Vec3 a = points.get(seg), b = points.get(seg + 1);
        double len = a.distanceTo(b);
        return len < 1e-6 ? b : a.lerp(b, along / len);
    }

    private void place() {
        Vec3 c = current();
        setPos(c.x, c.y - getBbHeight() / 2, c.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (points.size() < 2) {
            if (!level().isClientSide) discard();
            return;
        }
        double left = SPEED;
        while (left > 0 && seg < points.size() - 1) {
            double len = points.get(seg).distanceTo(points.get(seg + 1));
            double rem = len - along;
            if (left >= rem) {
                left -= rem;
                seg++;
                along = 0;
            } else {
                along += left;
                left = 0;
            }
        }
        place();
        if (level().isClientSide) return;
        if (getPassengers().isEmpty() && tickCount > 5) {
            discard();
            return;
        }
        for (Entity p : getPassengers()) p.fallDistance = 0;
        if (seg >= points.size() - 1) {
            arrive(exitPoint());
            return;
        }
        Block here = level().getBlockState(BlockPos.containing(current())).getBlock();
        if (!(here instanceof ViaductBlock) && !(here instanceof StationBlock)) arrive(current().subtract(0, 1.4, 0));
    }

    /** Standing spot at the destination station: on top if there is room, else beside it. */
    private Vec3 exitPoint() {
        Level l = level();
        BlockPos up = dest.above();
        if (free(up)) return new Vec3(up.getX() + 0.5, up.getY(), up.getZ() + 0.5);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos s = dest.relative(d);
            if (free(s) && !l.getBlockState(s.below()).isAir()) return new Vec3(s.getX() + 0.5, s.getY(), s.getZ() + 0.5);
        }
        return new Vec3(up.getX() + 0.5, up.getY(), up.getZ() + 0.5);
    }

    private boolean free(BlockPos p) {
        return level().getBlockState(p).getCollisionShape(level(), p).isEmpty()
                && level().getBlockState(p.above()).getCollisionShape(level(), p.above()).isEmpty();
    }

    private void arrive(Vec3 at) {
        arrived = true;
        for (Entity p : new ArrayList<>(getPassengers())) {
            p.stopRiding();
            p.teleportTo(at.x, at.y, at.z);
            p.fallDistance = 0;
        }
        level().playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.4f, 1.4f);
        discard();
    }

    @Override
    protected void positionRider(@Nonnull Entity passenger, @Nonnull MoveFunction move) {
        move.accept(passenger, getX(), getY() + getBbHeight() / 2 - passenger.getEyeHeight() + 0.05, getZ());
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        // client follows the path itself
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        ListTag l = new ListTag();
        for (Vec3 v : points) {
            CompoundTag t = new CompoundTag();
            t.putDouble("X", v.x);
            t.putDouble("Y", v.y);
            t.putDouble("Z", v.z);
            l.add(t);
        }
        tag.put("Points", l);
        tag.putInt("Seg", seg);
        tag.putDouble("Along", along);
        tag.put("Dest", NbtUtils.writeBlockPos(dest));
    }

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        points.clear();
        for (Tag e : tag.getList("Points", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) e;
            points.add(new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z")));
        }
        seg = tag.getInt("Seg");
        along = tag.getDouble("Along");
        dest = NbtUtils.readBlockPos(tag.getCompound("Dest"));
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeVarInt(points.size());
        for (Vec3 v : points) {
            buf.writeDouble(v.x);
            buf.writeDouble(v.y);
            buf.writeDouble(v.z);
        }
        buf.writeVarInt(seg);
        buf.writeDouble(along);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        points.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        seg = buf.readVarInt();
        along = buf.readDouble();
        place();
        setOldPosAndRot();
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
