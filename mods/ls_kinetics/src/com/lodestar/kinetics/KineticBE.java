package com.lodestar.kinetics;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.grid.GridNodeBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A member of a rotation network: axles, gears, junctions, pulleys, gauges and sources. Each node has one spin value
 * (its speed in RPM). On every face it exposes a shaft turning at {@link #faceFactor} times that spin, measured along
 * the positive direction of the face's axis; two nodes touching on shafts couple so their exposed spins match, and
 * gears beside each other mesh through {@link #meshFactor}. The network solves every node's speed as a multiple of
 * one base speed and syncs speed and phase to clients, which turn the parts smoothly between updates.
 */
public abstract class KineticBE extends GridNodeBE<KineticGrid> implements LSEntityBlock.IInteractable, LSEntityBlock.INeighborAware {
    public static final byte OK = 0, OVERLOADED = 1, JAMMED = 2;
    /** Degrees per tick per RPM (360 degrees / 1200 ticks). */
    public static final double DEG_PER_TICK = 0.3;

    protected float speed;
    protected byte status;
    protected float load;
    protected float phase;
    protected long t0;
    /** speed relative to the network's base speed (server only, set when the network is built) */
    double ratio = 1;

    protected KineticBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------ topology

    /** Spin of the shaft exposed on face d as a multiple of this node's spin (0 = no shaft there). */
    public abstract double faceFactor(Direction d);

    /** Spin multiplier to a neighbour beside this node that meshes with it (gear teeth), or 0. Must be symmetric. */
    public double meshFactor(Direction d, KineticBE other) {
        return 0;
    }

    /** Spin multiplier along a remote link (belt) to other. */
    public double remoteFactor(KineticBE other) {
        return 1;
    }

    /** Spin multiplier from this node to neighbour k across face d, or 0 when they are not coupled. */
    public double coupling(Direction d, KineticBE k) {
        double a = faceFactor(d), b = k.faceFactor(d.getOpposite());
        if (a != 0 && b != 0) return a / b;
        if (a == 0 && b == 0) return meshFactor(d, k);
        return 0;
    }

    @Override
    protected KineticGrid createGrid() {
        return new KineticGrid(level);
    }

    @Override
    public boolean linksTo(Direction d, GridNodeBE<?> other) {
        return other instanceof KineticBE k && coupling(d, k) != 0;
    }

    /** Whether this node's shaft on face d may drive a machine there (default: whenever a shaft is exposed). */
    public boolean drivesFace(Direction d) {
        return faceFactor(d) != 0;
    }

    // ------------------------------------------------------------------ state

    public float speed() {
        return speed;
    }

    public byte status() {
        return status;
    }

    public float load() {
        return load;
    }

    /** Whether the network load fraction is synced to clients for this node (gauges). */
    protected boolean syncsLoad() {
        return false;
    }

    /** Called by the network every tick; syncs clients when the speed, status (or, for gauges, the load) changes. */
    void applyNetwork(float newSpeed, byte newStatus, float util, double baseAngle, long now, boolean force) {
        boolean loadChanged = syncsLoad() && Math.abs(util - load) > 0.01f;
        load = util;
        if (force || Math.abs(newSpeed - speed) > 0.01f || newStatus != status || loadChanged) {
            phase = (float) ((baseAngle * ratio) % 360.0);
            t0 = now;
            speed = newSpeed;
            status = newStatus;
            onSpeedChanged();
            sync();
        }
    }

    protected void onSpeedChanged() {}

    /** Server: this node's angle (degrees) at game time now. */
    double angleNow(long now) {
        return phase + (now - t0) * speed * DEG_PER_TICK;
    }

    /** Client: current angle in degrees for rendering. */
    public float angle(float partialTick) {
        if (level == null) return 0;
        double t = (level.getGameTime() - t0) + partialTick;
        return (float) ((phase + t * speed * DEG_PER_TICK) % 360.0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        BlockState old = getBlockState();
        super.setBlockState(state);
        if (level != null && !level.isClientSide && old != state) invalidateGrid();
    }

    @Override
    public void onNeighborChanged(BlockPos from) {
        if (level == null || level.isClientSide) return;
        Direction d = directionTo(from);
        if (d == null || !drivesFace(d)) return;
        KineticGrid g = currentGrid();
        boolean wasEnd = g != null && g.hasEndAt(from);
        BlockEntity be = level.getBlockEntity(from);
        boolean isEnd = be instanceof IKineticConsumer c && !(be instanceof KineticBE) && c.acceptsRotation(d.getOpposite());
        if (wasEnd != isEnd) invalidateGrid();
    }

    protected Direction directionTo(BlockPos p) {
        BlockPos dp = p.subtract(worldPosition);
        return Direction.fromDelta(dp.getX(), dp.getY(), dp.getZ());
    }

    protected Direction.Axis axis() {
        BlockState s = getBlockState();
        if (s.hasProperty(BlockStateProperties.AXIS)) return s.getValue(BlockStateProperties.AXIS);
        return LSEntityBlock.facingOf(s).getAxis();
    }

    protected Direction facing() {
        return LSEntityBlock.facingOf(getBlockState());
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (level != null && !level.isClientSide) player.displayClientMessage(describe(), true);
        return InteractionResult.sidedSuccess(level != null && level.isClientSide);
    }

    /** One line about this part and its network: speed, load and trouble. */
    public Component describe() {
        MutableComponent c = Component.translatable("msg.ls_kinetics.speed", fmt(speed)).withStyle(ChatFormatting.YELLOW);
        KineticGrid g = currentGrid();
        if (g != null) {
            c.append(Component.literal("  "));
            c.append(Component.translatable("msg.ls_kinetics.load", Math.round(g.demand), Math.round(g.capacity),
                    g.capacity > 0 ? Math.round(100 * g.demand / g.capacity) : 0).withStyle(ChatFormatting.GRAY));
        }
        if (status == OVERLOADED) c.append(Component.literal("  ")).append(Component.translatable("msg.ls_kinetics.overloaded").withStyle(ChatFormatting.RED));
        if (status == JAMMED) c.append(Component.literal("  ")).append(Component.translatable("msg.ls_kinetics.jammed").withStyle(ChatFormatting.RED));
        return c;
    }

    public static String fmt(float rpm) {
        float a = Math.abs(rpm);
        return a >= 10 || a == Math.round(a) ? String.valueOf(Math.round(rpm)) : String.format("%.1f", rpm);
    }

    // ------------------------------------------------------------------ client

    @Override
    public void tickClient() {
        if (level == null || status == OK) return;
        if (level.random.nextInt(status == OVERLOADED ? 30 : 15) != 0) return;
        double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
        double y = worldPosition.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
        double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
        level.addParticle(status == OVERLOADED ? ParticleTypes.SMOKE : ParticleTypes.CRIT, x, y, z, 0, 0.02, 0);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveSync(CompoundTag tag) {
        saveAll(tag);
        tag.putFloat("Spd", speed);
        tag.putByte("St", status);
        tag.putFloat("Ph", phase);
        tag.putLong("T0", t0);
        if (syncsLoad()) tag.putFloat("Ld", load);
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        loadAll(tag);
        speed = tag.getFloat("Spd");
        status = tag.getByte("St");
        phase = tag.getFloat("Ph");
        t0 = tag.getLong("T0");
        load = tag.getFloat("Ld");
    }
}
