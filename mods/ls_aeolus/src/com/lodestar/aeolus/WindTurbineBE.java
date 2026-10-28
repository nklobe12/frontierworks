package com.lodestar.aeolus;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.item.AugmentItem;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Wind turbine. Output = rotor rating x height above sea level x wind strength x weather x how well the rotor faces the wind x
 * how open the air around it is. The swept circle of the rotor must be completely clear, and rotors in the same plane must not
 * overlap. The rotor wears while it turns and breaks when worn out.
 */
public class WindTurbineBE extends MachineBlockEntity implements LSEntityBlock.IInteractable {
    public static final int SLOT_ROTOR = 0, SLOT_CHARGE = 1;
    public static final int OK = 0, NO_ROTOR = 1, BLOCKED = 2, COLLIDE = 3, LOW = 4, NO_WIND = 5;
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .slot(17, 50, MachineLayout.Role.INPUT).slot(138, 55, MachineLayout.Role.CHARGE)
            .energy(160, 17, 52).info(48, 17).extra(8).build();
    private static final int CAPACITY = 40_000, PUSH = 500;
    /** Server-side turbines currently loaded, for rotor overlap checks. */
    private static final Set<WindTurbineBE> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    public int status = NO_ROTOR;
    public int blocked;
    public double clearance = 1;
    private int sweepTimer, fetchTimer, wearTimer;
    private double heightF;

    // client view
    public Item rotorItem = Items.AIR;
    public float targetSpeed, speed, angle, prevAngle;
    private float syncedSpeed = -1;

    public WindTurbineBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, CAPACITY, 0, PUSH);
    }

    @Override
    public boolean hasSideConfig() {
        return false;
    }

    @Override
    public boolean acceptsAugment(AugmentItem a) {
        return false;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_ROTOR) return stack.getItem() instanceof RotorItem;
        return super.isItemValid(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == SLOT_ROTOR ? 1 : super.getSlotLimit(slot);
    }

    public RotorItem rotor() {
        return items.getStackInSlot(SLOT_ROTOR).getItem() instanceof RotorItem r ? r : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED.remove(this);
    }

    // ------------------------------------------------------------------ area checks

    private boolean clear(BlockPos p) {
        if (!level.isLoaded(p)) return true;
        BlockState st = level.getBlockState(p);
        return st.isAir() || st.getCollisionShape(level, p).isEmpty();
    }

    private BlockPos hub() {
        return worldPosition.relative(facing());
    }

    /** Counts blocks inside the circle the rotor sweeps (it turns in the plane of the block in front). */
    private int sweep(int radius) {
        Direction f = facing(), across = f.getClockWise();
        BlockPos hub = hub();
        double lim = (radius + 0.5) * (radius + 0.5);
        int n = 0;
        for (int a = -radius; a <= radius; a++)
            for (int b = -radius; b <= radius; b++) {
                if (a * a + b * b > lim) continue;
                if (!clear(hub.relative(across, a).relative(Direction.UP, b))) n++;
            }
        return n;
    }

    /** Fraction of open air in the column the wind passes through, eight blocks in front of the rotor and behind the turbine. */
    private double fetch(int radius) {
        Direction f = facing(), across = f.getClockWise();
        double lim = (radius + 0.5) * (radius + 0.5);
        int solid = 0, total = 0;
        for (int d = 1; d <= 8; d++) {
            BlockPos front = hub().relative(f, d), back = worldPosition.relative(f.getOpposite(), d);
            for (int a = -radius; a <= radius; a++)
                for (int b = -radius; b <= radius; b++) {
                    if (a * a + b * b > lim) continue;
                    total += 2;
                    if (!clear(front.relative(across, a).relative(Direction.UP, b))) solid++;
                    if (!clear(back.relative(across, a).relative(Direction.UP, b))) solid++;
                }
        }
        return total == 0 ? 1 : Math.max(0, Math.min(1, 1 - 1.6 * solid / total));
    }

    /** Another turbine whose rotor turns in the same plane and would touch ours. */
    private boolean collides(RotorItem mine) {
        Direction f = facing();
        BlockPos hub = hub();
        for (WindTurbineBE o : LOADED) {
            if (o == this || o.isRemoved() || o.level != level) continue;
            RotorItem r = o.rotor();
            if (r == null || o.facing().getAxis() != f.getAxis()) continue;
            BlockPos oh = o.hub();
            int along = f.getAxis().choose(oh.getX() - hub.getX(), oh.getY() - hub.getY(), oh.getZ() - hub.getZ());
            if (along != 0) continue;
            double dx = oh.getX() - hub.getX(), dy = oh.getY() - hub.getY(), dz = oh.getZ() - hub.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < mine.radius + r.radius + 1) return true;
        }
        return false;
    }

    private void rescan(RotorItem r) {
        heightF = Wind.height(level, worldPosition);
        if (r == null) {
            status = NO_ROTOR;
            blocked = 0;
            return;
        }
        if (!Wind.hasWind(level)) {
            status = NO_WIND;
            return;
        }
        if (heightF <= 0) {
            status = LOW;
            return;
        }
        blocked = sweep(r.radius);
        if (blocked > 0) {
            status = BLOCKED;
            return;
        }
        status = collides(r) ? COLLIDE : OK;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        RotorItem r = rotor();
        if (inventoryDirty || ++sweepTimer >= 20) {
            sweepTimer = 0;
            boolean changed = inventoryDirty;
            inventoryDirty = false;
            rescan(r);
            if (r != null && (changed || ++fetchTimer >= 5)) {
                fetchTimer = 0;
                clearance = fetch(r.radius);
            }
        }
        int made = 0;
        if (r != null && status == OK && canRun() && !energy.isFull()) {
            double w = Wind.strength(level) * Wind.weather(level) * Wind.alignment(facing(), Wind.bearing(level));
            made = (int) Math.round(r.power * heightF * w * clearance);
            if (made > 0) {
                energy.produce(made);
                if (++wearTimer >= 20) {
                    wearTimer = 0;
                    wear(level.isThundering() ? 2 : 1);
                }
            }
        }
        rate = made;
        chargeItems(PUSH);
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, PUSH, d -> d != facing());
        setLit(made > 0);
        r = rotor();
        float want = r == null || made <= 0 ? 0 : 3f + 12f * Math.min(1.5f, made / (float) r.power);
        Item shown = items.getStackInSlot(SLOT_ROTOR).getItem();
        if (shown != rotorItem || Math.abs(want - syncedSpeed) > 0.6f) {
            rotorItem = shown;
            targetSpeed = want;
            syncedSpeed = want;
            sync();
        }
        setChanged();
    }

    private void wear(int amount) {
        ItemStack st = items.getStackInSlot(SLOT_ROTOR);
        if (st.isEmpty()) return;
        int d = st.getDamageValue() + amount;
        if (d >= st.getMaxDamage()) {
            items.setStackInSlot(SLOT_ROTOR, ItemStack.EMPTY);
            level.playSound((Player) null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
        } else {
            ItemStack n = st.copy();
            n.setDamageValue(d);
            items.setStackInSlot(SLOT_ROTOR, n);
        }
    }

    @Override
    public void clientTick() {
        speed += (targetSpeed - speed) * 0.04f;
        if (angle > 3600f) {
            angle -= 3600f;
        }
        prevAngle = angle;
        angle += speed;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof RotorItem && items.getStackInSlot(SLOT_ROTOR).isEmpty()) {
            if (!level.isClientSide) {
                items.setStackInSlot(SLOT_ROTOR, held.split(1));
                level.playSound((Player) null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.8f, 1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ screen data

    @Override
    public int getExtra(int i) {
        if (level == null) return 0;
        return switch (i) {
            case 0 -> rate;
            case 1 -> status;
            case 2 -> (int) Math.round(heightF * 100);
            case 3 -> (int) Math.round(Wind.strength(level) * Wind.weather(level) * 100);
            case 4 -> (int) Math.round(clearance * 100);
            case 5 -> blocked;
            case 6 -> (int) Math.round(Math.toDegrees(Wind.bearing(level)));
            default -> (int) Math.round(Wind.alignment(facing(), Wind.bearing(level)) * 100);
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_aeolus.output", menu.extra(0)));
        double local = menu.extra(3) / 100.0 * menu.extra(2) / 100.0;
        lines.add(Component.translatable("gui.ls_aeolus.wind", String.format("%.1f", Wind.metres(local)),
                Wind.compass(Math.toRadians(menu.extra(6)))));
        lines.add(Component.translatable("gui.ls_aeolus.height", String.format("%.2f", menu.extra(2) / 100.0)));
        lines.add(statusLine(menu.extra(1), menu.extra(5), menu.extra(4)));
    }

    public static Component statusLine(int status, int blocked, int clearPct) {
        return switch (status) {
            case OK -> Component.translatable("gui.ls_aeolus.clear", clearPct).withStyle(clearPct < 70 ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN);
            case NO_ROTOR -> Component.translatable("gui.ls_aeolus.status.no_rotor").withStyle(ChatFormatting.DARK_RED);
            case BLOCKED -> Component.translatable("gui.ls_aeolus.status.blocked", blocked).withStyle(ChatFormatting.DARK_RED);
            case COLLIDE -> Component.translatable("gui.ls_aeolus.status.collide").withStyle(ChatFormatting.DARK_RED);
            case LOW -> Component.translatable("gui.ls_aeolus.status.low").withStyle(ChatFormatting.DARK_RED);
            default -> Component.translatable("gui.ls_aeolus.status.no_wind").withStyle(ChatFormatting.DARK_RED);
        };
    }

    // ------------------------------------------------------------------ sync / persistence

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putDouble("Clearance", clearance);
        tag.putInt("Wear", wearTimer);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        clearance = tag.contains("Clearance") ? tag.getDouble("Clearance") : 1;
        wearTimer = tag.getInt("Wear");
    }

    @Override
    protected void saveSyncExtra(CompoundTag tag) {
        tag.putString("Rotor", BuiltInRegistries.ITEM.getKey(items.getStackInSlot(SLOT_ROTOR).getItem()).toString());
        tag.putFloat("Speed", targetSpeed);
    }

    @Override
    protected void loadSyncExtra(CompoundTag tag) {
        if (tag.contains("Rotor")) rotorItem = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("Rotor")));
        targetSpeed = tag.getFloat("Speed");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5);
    }
}
