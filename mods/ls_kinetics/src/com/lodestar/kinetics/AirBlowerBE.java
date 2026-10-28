package com.lodestar.kinetics;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Air blower: a shaft on any face but the front spins a propeller that blows a stream of air forward, one block per
 * 8 RPM up to 16 blocks, stopped by full blocks. The stream pushes mobs and items (turn it the other way to pull).
 * Items carried through a block of water, fire, lava or soul fire in the stream are rinsed, roasted, seared or steeped.
 */
public class AirBlowerBE extends LSBlockEntity implements IKineticConsumer, LSEntityBlock.IInteractable {
    public static final float TORQUE = 32;
    public static final int MAX_RANGE = 16;
    private static final String TAG = "ls_kinetics_air";
    private float rpm;
    private byte driveStatus;
    private long lastDrive = -10;
    private int range;
    private int catalystAt = -1;
    private AirProcessing.Type catalyst = AirProcessing.Type.NONE;
    private float syncedRpm;
    public float clientAngle, clientAnglePrev;

    public AirBlowerBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Direction facing() {
        return LSEntityBlock.facingOf(getBlockState());
    }

    @Override
    public boolean acceptsRotation(Direction side) {
        return side != facing();
    }

    @Override
    public float torqueDemand() {
        return TORQUE;
    }

    @Override
    public void setRotation(Direction side, float rpm, byte status) {
        this.rpm = rpm;
        this.driveStatus = status;
        if (level != null) lastDrive = level.getGameTime();
    }

    public float rpm() {
        return rpm;
    }

    public int range() {
        return range;
    }

    public AirProcessing.Type catalyst() {
        return catalyst;
    }

    public int catalystAt() {
        return catalystAt;
    }

    /** Reach of the stream for a shaft speed. */
    public static int reach(float rpm) {
        float a = Math.abs(rpm);
        return a < 1 ? 0 : Math.min(MAX_RANGE, 1 + (int) (a / 8));
    }

    private void scan() {
        int want = reach(rpm);
        Direction f = facing();
        int len = 0;
        int cat = -1;
        AirProcessing.Type type = AirProcessing.Type.NONE;
        for (int i = 1; i <= want; i++) {
            BlockPos p = worldPosition.relative(f, i);
            if (!level.isLoaded(p)) break;
            BlockState s = level.getBlockState(p);
            AirProcessing.Type t = AirProcessing.catalyst(s);
            if (t == AirProcessing.Type.NONE && s.isCollisionShapeFullBlock(level, p)) break;
            len = i;
            if (cat < 0 && t != AirProcessing.Type.NONE) {
                cat = i;
                type = t;
            }
        }
        if (len != range || cat != catalystAt || type != catalyst) {
            range = len;
            catalystAt = cat;
            catalyst = type;
            sync();
        }
    }

    /** The stream's box from the first cell to the far end (block coordinates). */
    private AABB streamBox(int from, int to) {
        Direction f = facing();
        BlockPos a = worldPosition.relative(f, from), b = worldPosition.relative(f, to);
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    @Override
    public void tickServer() {
        if (level == null) return;
        long now = level.getGameTime();
        if (now - lastDrive > 1) {
            rpm = 0;
            driveStatus = KineticBE.OK;
        }
        if (Math.abs(rpm - syncedRpm) > 0.5f) {
            syncedRpm = rpm;
            sync();
        }
        if (now % 10 == Math.floorMod(worldPosition.hashCode(), 10) || (rpm != 0 && range == 0) || (rpm == 0 && range != 0)) scan();
        if (range <= 0 || rpm == 0) return;
        push();
        if (catalyst != AirProcessing.Type.NONE) processItems();
    }

    private void push() {
        Direction f = facing();
        Vec3 dir = Vec3.atLowerCornerOf(f.getNormal()).scale(rpm > 0 ? 1 : -1);
        double strength = Math.min(0.12, 0.02 + Math.abs(rpm) / 256.0 * 0.1);
        List<Entity> list = level.getEntities((Entity) null, streamBox(1, range), e -> !e.isSpectator() && !(e instanceof Player p && p.getAbilities().flying));
        Vec3 origin = Vec3.atCenterOf(worldPosition);
        for (Entity e : list) {
            double dist = e.position().distanceTo(origin);
            double fall = Math.max(0.2, 1 - dist / (range + 1));
            Vec3 v = e.getDeltaMovement();
            double along = v.dot(dir);
            if (along > 0.6) continue;
            Vec3 add = dir.scale(strength * fall);
            e.setDeltaMovement(v.add(add));
            if (f == Direction.UP && rpm > 0) e.resetFallDistance();
            e.hurtMarked = true;
        }
    }

    private void processItems() {
        float speedFactor = Math.max(0.5f, Math.abs(rpm) / 64f);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, streamBox(Math.max(1, catalystAt), range));
        for (ItemEntity ie : items) {
            if (!ie.isAlive()) continue;
            CompoundTag d = ie.getPersistentData().getCompound(TAG);
            int type = d.getInt("T");
            float prog = d.getFloat("P");
            if (type != catalyst.ordinal()) {
                type = catalyst.ordinal();
                prog = AirProcessing.canProcess(level, catalyst, ie.getItem()) ? 0 : -1;
            }
            if (prog < 0) {
                d.putInt("T", type);
                d.putFloat("P", -1);
                ie.getPersistentData().put(TAG, d);
                continue;
            }
            prog += speedFactor;
            if (prog >= AirProcessing.TIME) {
                List<ItemStack> out = AirProcessing.process(level, catalyst, ie.getItem());
                if (out != null) {
                    for (ItemStack s : out) {
                        ItemEntity n = new ItemEntity(level, ie.getX(), ie.getY(), ie.getZ(), s);
                        n.setDeltaMovement(ie.getDeltaMovement());
                        CompoundTag nd = new CompoundTag();
                        nd.putInt("T", type);
                        nd.putFloat("P", -1);
                        n.getPersistentData().put(TAG, nd);
                        level.addFreshEntity(n);
                    }
                    ie.discard();
                    burst(ie.position());
                    continue;
                }
                prog = -1;
            }
            d.putInt("T", type);
            d.putFloat("P", prog);
            ie.getPersistentData().put(TAG, d);
        }
    }

    private void burst(Vec3 at) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(particle(catalyst), at.x, at.y + 0.2, at.z, 6, 0.15, 0.1, 0.15, 0.02);
        }
    }

    static ParticleOptions particle(AirProcessing.Type t) {
        return switch (t) {
            case RINSE -> ParticleTypes.SPLASH;
            case ROAST -> ParticleTypes.SMOKE;
            case SEAR -> ParticleTypes.FLAME;
            case STEEP -> ParticleTypes.SOUL_FIRE_FLAME;
            case NONE -> ParticleTypes.CLOUD;
        };
    }

    @Override
    public void tickClient() {
        clientAnglePrev = clientAngle;
        clientAngle = (float) ((clientAngle + rpm * KineticBE.DEG_PER_TICK * 2) % 360.0);
        if (level == null || range <= 0 || rpm == 0 || level.random.nextInt(3) != 0) return;
        Direction f = facing();
        Vec3 dir = Vec3.atLowerCornerOf(f.getNormal());
        double d = 0.6 + level.random.nextDouble() * range;
        Vec3 c = Vec3.atCenterOf(worldPosition).add(dir.scale(d));
        double jx = (level.random.nextDouble() - 0.5) * 0.7, jy = (level.random.nextDouble() - 0.5) * 0.7, jz = (level.random.nextDouble() - 0.5) * 0.7;
        Vec3 v = dir.scale(0.08 * Math.signum(rpm));
        boolean past = catalystAt > 0 && d >= catalystAt - 0.5;
        level.addParticle(past ? particle(catalyst) : ParticleTypes.POOF, c.x + jx * (1 - Math.abs(dir.x)), c.y + jy * (1 - Math.abs(dir.y)),
                c.z + jz * (1 - Math.abs(dir.z)), v.x, v.y, v.z);
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() || level == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Component c = driveStatus == KineticBE.OVERLOADED ? Component.translatable("msg.ls_kinetics.overloaded").withStyle(ChatFormatting.RED)
                    : Component.translatable("msg.ls_kinetics.blower", KineticBE.fmt(Math.abs(rpm)), range,
                    Component.translatable("air.ls_kinetics." + catalyst.name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.YELLOW);
            player.displayClientMessage(c, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }

    @Override
    protected void saveAll(CompoundTag tag) {}

    @Override
    protected void loadAll(CompoundTag tag) {}

    @Override
    protected void saveSync(CompoundTag tag) {
        tag.putFloat("Rpm", rpm);
        tag.putInt("Range", range);
        tag.putInt("CatAt", catalystAt);
        tag.putInt("Cat", catalyst.ordinal());
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        rpm = tag.getFloat("Rpm");
        range = tag.getInt("Range");
        catalystAt = tag.getInt("CatAt");
        AirProcessing.Type[] v = AirProcessing.Type.values();
        catalyst = v[Math.max(0, Math.min(v.length - 1, tag.getInt("Cat")))];
    }
}
