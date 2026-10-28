package com.lodestar.kinetics;

import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Windmill hub. Build a sail of windmill vanes on the face in front of the hub (vanes joined edge to edge, flat in one
 * plane, within 8 blocks of the axle) and use the hub to raise it: the vanes come off the wall and turn with the hub.
 * Wind is stronger high up and in rain or thunder, and only vanes open to the sky catch it. Speed grows with sail size
 * up to 16 RPM; torque is 24 Nm per vane at full wind. The swept circle must stay clear or the sail stops. Use the hub
 * again to reverse it; sneak-use it to take the sail down.
 */
public class WindmillHubBE extends KineticBE implements IKineticSource, LSEntityBlock.IDropContents {
    public static final int MIN_VANES = 8, MAX_VANES = 160, RADIUS = 8;
    private final List<int[]> vanes = new ArrayList<>();
    private float wind;
    private float radius;
    private boolean obstructed;
    private int dir = 1;

    public WindmillHubBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public double faceFactor(Direction d) {
        return d == facing().getOpposite() ? 1 : 0;
    }

    public List<int[]> vanes() {
        return vanes;
    }

    public boolean assembled() {
        return !vanes.isEmpty();
    }

    /** Horizontal unit across the sail plane. */
    public Direction across() {
        return facing().getClockWise();
    }

    /** World position of the sail cell (u across, v up). */
    public BlockPos cell(int u, int v) {
        return worldPosition.relative(facing()).relative(across(), u).above(v);
    }

    @Override
    public float sourceSpeed() {
        if (vanes.isEmpty() || obstructed || wind < 0.05f) return 0;
        float base = Math.max(4, Math.min(16, 4 + vanes.size() / 8f));
        return Math.max(1, Math.round(base * Math.min(1.25f, wind))) * dir;
    }

    @Override
    public float sourceTorque() {
        return vanes.isEmpty() || obstructed ? 0 : 24 * vanes.size() * Math.min(1.6f, wind);
    }

    // ------------------------------------------------------------------ assembly

    /** Collects the vanes in front of the hub and lifts them into the rotor. Returns a message key on failure. */
    public String assemble() {
        if (level == null) return "msg.ls_kinetics.windmill_none";
        Set<Long> seen = new HashSet<>();
        List<int[]> found = new ArrayList<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (!isVane(cell(0, 0))) return "msg.ls_kinetics.windmill_none";
        queue.add(new int[]{0, 0});
        seen.add(key(0, 0));
        while (!queue.isEmpty() && found.size() <= MAX_VANES) {
            int[] c = queue.poll();
            found.add(c);
            int[][] next = {{c[0] + 1, c[1]}, {c[0] - 1, c[1]}, {c[0], c[1] + 1}, {c[0], c[1] - 1}};
            for (int[] n : next) {
                if (n[0] * n[0] + n[1] * n[1] > RADIUS * RADIUS || !seen.add(key(n[0], n[1]))) continue;
                if (isVane(cell(n[0], n[1]))) queue.add(n);
            }
        }
        if (found.size() > MAX_VANES) return "msg.ls_kinetics.windmill_big";
        if (found.size() < MIN_VANES) return "msg.ls_kinetics.windmill_small";
        vanes.clear();
        vanes.addAll(found);
        for (int[] c : vanes) level.setBlock(cell(c[0], c[1]), Blocks.AIR.defaultBlockState(), 3);
        measure();
        refreshWind();
        invalidateGrid();
        sync();
        level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 0.7f);
        return null;
    }

    /** Puts the sail back on the wall (vanes that have no room drop as items). */
    public void disassemble() {
        if (level == null || vanes.isEmpty()) return;
        BlockState vane = Content.WINDMILL_VANE.get().defaultBlockState();
        for (int[] c : vanes) {
            BlockPos p = cell(c[0], c[1]);
            if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, vane, 3);
            else Containers.dropItemStack(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, new ItemStack(Content.WINDMILL_VANE.get()));
        }
        vanes.clear();
        wind = 0;
        invalidateGrid();
        sync();
    }

    private boolean isVane(BlockPos p) {
        return level.getBlockState(p).is(Content.WINDMILL_VANE.get());
    }

    private static long key(int u, int v) {
        return ((long) u << 32) ^ (v & 0xffffffffL);
    }

    private void measure() {
        float r = 0;
        for (int[] c : vanes) r = Math.max(r, (float) Math.sqrt(c[0] * c[0] + c[1] * c[1]));
        radius = r;
    }

    /** Wind at the hub: height, weather, and the share of vanes open to the sky. Also checks the swept circle. */
    void refreshWind() {
        if (level == null || vanes.isEmpty()) return;
        int ri = (int) Math.ceil(radius);
        boolean blocked = false;
        for (int u = -ri; u <= ri && !blocked; u++) {
            for (int v = -ri; v <= ri; v++) {
                if (u * u + v * v > (radius + 0.5f) * (radius + 0.5f)) continue;
                BlockState s = level.getBlockState(cell(u, v));
                if (!s.isAir() && !s.canBeReplaced()) {
                    blocked = true;
                    break;
                }
            }
        }
        obstructed = blocked;
        int open = 0;
        for (int[] c : vanes) if (level.canSeeSky(cell(c[0], c[1]))) open++;
        float exposure = (float) open / vanes.size();
        float height = Math.max(0.2f, Math.min(1.3f, 0.5f + (worldPosition.getY() - 64) / 96f));
        float weather = level.isThundering() ? 1.6f : level.isRaining() ? 1.3f : 1f;
        float dim = level.dimensionType().hasCeiling() ? 0.25f : 1f;
        float w = height * weather * exposure * dim;
        if (Math.abs(w - wind) > 0.01f) {
            wind = w;
            sync();
        }
    }

    @Override
    protected void nodeTick() {
        if (level != null && !vanes.isEmpty() && (level.getGameTime() + Math.floorMod(worldPosition.hashCode(), 40)) % 40 == 0) refreshWind();
    }

    public float wind() {
        return wind;
    }

    public boolean obstructed() {
        return obstructed;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() || level == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (vanes.isEmpty()) {
            String err = assemble();
            if (err != null) player.displayClientMessage(Component.translatable(err, MIN_VANES, MAX_VANES).withStyle(ChatFormatting.RED), true);
            else player.displayClientMessage(describe(), true);
        } else if (player.isShiftKeyDown()) {
            disassemble();
        } else {
            dir = -dir;
            invalidateGrid();
            player.displayClientMessage(describe(), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component describe() {
        if (vanes.isEmpty()) return Component.translatable("msg.ls_kinetics.windmill_idle").withStyle(ChatFormatting.GRAY);
        if (obstructed) return Component.translatable("msg.ls_kinetics.windmill_blocked").withStyle(ChatFormatting.RED);
        return Component.translatable("msg.ls_kinetics.windmill", vanes.size(), Math.round(wind * 100), fmt(Math.abs(sourceSpeed())),
                Math.round(sourceTorque())).withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        int n = vanes.size();
        while (n > 0) {
            int c = Math.min(64, n);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Content.WINDMILL_VANE.get(), c));
            n -= c;
        }
        vanes.clear();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(RADIUS + 1.5);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        ListTag l = new ListTag();
        for (int[] c : vanes) l.add(new IntArrayTag(new int[]{c[0], c[1]}));
        tag.put("Vanes", l);
        tag.putFloat("Wind", wind);
        tag.putBoolean("Blocked", obstructed);
        tag.putInt("Dir", dir);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        vanes.clear();
        for (Tag t : tag.getList("Vanes", Tag.TAG_INT_ARRAY)) {
            int[] a = ((IntArrayTag) t).getAsIntArray();
            if (a.length == 2) vanes.add(a);
        }
        wind = tag.getFloat("Wind");
        obstructed = tag.getBoolean("Blocked");
        dir = tag.getInt("Dir") == -1 ? -1 : 1;
        measure();
    }
}
