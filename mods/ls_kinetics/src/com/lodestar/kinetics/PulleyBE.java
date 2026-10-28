package com.lodestar.kinetics;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * A pulley on its axle. A drive belt joins two pulleys on parallel axles up to {@link #MAX_SPAN} blocks apart, so both
 * turn the same way at the same speed. Breaking either pulley drops the belt.
 */
public class PulleyBE extends KineticBE implements LSEntityBlock.IDropContents {
    public static final int MAX_SPAN = 24;
    @Nullable
    private BlockPos partner;

    public PulleyBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public BlockPos partner() {
        return partner;
    }

    @Override
    public double faceFactor(Direction d) {
        return d.getAxis() == axis() ? 1 : 0;
    }

    @Override
    public Collection<BlockPos> remoteLinks() {
        return partner == null ? List.of() : List.of(partner);
    }

    /** Why a belt from this pulley to other cannot be fitted (lang key), or null if it can. */
    @Nullable
    public String canLink(PulleyBE other) {
        if (other == this) return "msg.ls_kinetics.belt_same";
        if (partner != null || other.partner != null) return "msg.ls_kinetics.belt_taken";
        if (other.axis() != axis()) return "msg.ls_kinetics.belt_axis";
        BlockPos d = other.worldPosition.subtract(worldPosition);
        int along = switch (axis()) {
            case X -> d.getX();
            case Y -> d.getY();
            case Z -> d.getZ();
        };
        if (along != 0) return "msg.ls_kinetics.belt_plane";
        double len = Math.sqrt(d.distSqr(BlockPos.ZERO));
        if (len < 2) return "msg.ls_kinetics.belt_close";
        if (len > MAX_SPAN) return "msg.ls_kinetics.belt_far";
        return null;
    }

    public void link(PulleyBE other) {
        partner = other.worldPosition;
        other.partner = worldPosition;
        invalidateGrid();
        other.invalidateGrid();
        sync();
        other.sync();
    }

    void unlink() {
        partner = null;
        invalidateGrid();
        sync();
    }

    /** Whether this pulley draws the belt (the lower of the two positions does). */
    public boolean drawsBelt() {
        return partner != null && worldPosition.compareTo(partner) < 0;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (partner == null) return;
        if (level.isLoaded(partner) && level.getBlockEntity(partner) instanceof PulleyBE o && worldPosition.equals(o.partner)) o.unlink();
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Content.DRIVE_BELT.get()));
        partner = null;
    }

    @Override
    public void tickServer() {
        super.tickServer();
        // a partner that vanished without telling us (e.g. replaced by another block) frees the belt
        if (partner != null && level != null && level.getGameTime() % 40 == Math.floorMod(worldPosition.hashCode(), 40) &&level.isLoaded(partner)
                && !(level.getBlockEntity(partner) instanceof PulleyBE o && worldPosition.equals(o.partner))) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(Content.DRIVE_BELT.get()));
            unlink();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB box = new AABB(worldPosition).inflate(0.5);
        if (partner != null) box = box.minmax(new AABB(partner).inflate(0.5));
        return box;
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        if (partner != null) tag.put("Partner", NbtUtils.writeBlockPos(partner));
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        partner = tag.contains("Partner") ? NbtUtils.readBlockPos(tag.getCompound("Partner")) : null;
    }
}
