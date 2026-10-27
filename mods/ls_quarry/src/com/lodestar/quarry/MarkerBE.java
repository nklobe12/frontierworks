package com.lodestar.quarry;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Survey marker. Three markers in an L on one level mark the corners of a quarry frame: using one searches along X and Z
 * for partners and stores the rectangle in all of them. The corner marker draws laser lines along the edges.
 */
public class MarkerBE extends LSBlockEntity implements LSEntityBlock.IInteractable, LSEntityBlock.IPlacedBy {
    public static final int RANGE = 64;

    /** rectangle of the frame (inclusive corners), or null */
    @Nullable
    private BlockPos min, max;
    /** markers this one linked to (drawn as beams from here) */
    @Nullable
    private BlockPos partnerX, partnerZ;

    public MarkerBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public BlockPos areaMin() {
        return min;
    }

    @Nullable
    public BlockPos areaMax() {
        return max;
    }

    @Nullable
    public BlockPos partnerX() {
        return partnerX;
    }

    @Nullable
    public BlockPos partnerZ() {
        return partnerZ;
    }

    public boolean hasArea() {
        return min != null && max != null;
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        if (level != null && !level.isClientSide) link(placer instanceof Player p ? p : null, true);
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level != null && !level.isClientSide) link(player, false);
        return InteractionResult.sidedSuccess(level != null && level.isClientSide);
    }

    @Nullable
    private BlockPos find(Direction dir) {
        for (int i = 1; i <= RANGE; i++) {
            BlockPos p = worldPosition.relative(dir, i);
            if (!level.isLoaded(p)) return null;
            if (level.getBlockEntity(p) instanceof MarkerBE) return p;
        }
        return null;
    }

    /** Looks for partner markers along X and Z; stores the rectangle in all three when both are found. */
    public void link(@Nullable Player player, boolean quiet) {
        BlockPos px = find(Direction.EAST);
        if (px == null) px = find(Direction.WEST);
        BlockPos pz = find(Direction.SOUTH);
        if (pz == null) pz = find(Direction.NORTH);
        if (px == null && pz == null) {
            if (player != null && !quiet) player.displayClientMessage(Component.translatable("msg.ls_quarry.none"), true);
            return;
        }
        partnerX = px;
        partnerZ = pz;
        if (px == null || pz == null) {
            sync();
            if (player != null && !quiet) player.displayClientMessage(Component.translatable("msg.ls_quarry.partial"), true);
            return;
        }
        BlockPos a = new BlockPos(Math.min(px.getX(), worldPosition.getX()), worldPosition.getY(), Math.min(pz.getZ(), worldPosition.getZ()));
        BlockPos b = new BlockPos(Math.max(px.getX(), worldPosition.getX()), worldPosition.getY(), Math.max(pz.getZ(), worldPosition.getZ()));
        if (b.getX() - a.getX() < 2 || b.getZ() - a.getZ() < 2) {
            if (player != null) player.displayClientMessage(Component.translatable("msg.ls_quarry.too_small"), true);
            return;
        }
        setArea(a, b);
        for (BlockPos p : new BlockPos[]{px, pz}) {
            if (level.getBlockEntity(p) instanceof MarkerBE other) {
                other.partnerX = null;
                other.partnerZ = null;
                other.setArea(a, b);
            }
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("msg.ls_quarry.linked", b.getX() - a.getX() + 1, b.getZ() - a.getZ() + 1), true);
        }
    }

    private void setArea(BlockPos a, BlockPos b) {
        min = a;
        max = b;
        sync();
    }

    /** Positions of the markers belonging to this marker's rectangle (this one and its partners). */
    public BlockPos[] group() {
        return new BlockPos[]{worldPosition, partnerX, partnerZ};
    }

    /** Finds a marker with a complete rectangle next to pos (the quarry). */
    @Nullable
    public static MarkerBE adjacent(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(d));
            if (be instanceof MarkerBE m && m.hasArea()) return m;
        }
        return null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (min != null && max != null) {
            return new AABB(min.getX() - 1, level == null ? -64 : level.getMinBuildHeight(), min.getZ() - 1, max.getX() + 2, max.getY() + 3, max.getZ() + 2);
        }
        return new AABB(worldPosition).inflate(RANGE, 2, RANGE);
    }

    @Override
    protected boolean isTicking() {
        return false;
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        if (min != null && max != null) {
            tag.put("Min", NbtUtils.writeBlockPos(min));
            tag.put("Max", NbtUtils.writeBlockPos(max));
        }
        if (partnerX != null) tag.put("PX", NbtUtils.writeBlockPos(partnerX));
        if (partnerZ != null) tag.put("PZ", NbtUtils.writeBlockPos(partnerZ));
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        min = tag.contains("Min") ? NbtUtils.readBlockPos(tag.getCompound("Min")) : null;
        max = tag.contains("Max") ? NbtUtils.readBlockPos(tag.getCompound("Max")) : null;
        partnerX = tag.contains("PX") ? NbtUtils.readBlockPos(tag.getCompound("PX")) : null;
        partnerZ = tag.contains("PZ") ? NbtUtils.readBlockPos(tag.getCompound("PZ")) : null;
    }
}
