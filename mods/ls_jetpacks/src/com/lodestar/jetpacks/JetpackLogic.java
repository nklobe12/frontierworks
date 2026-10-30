package com.lodestar.jetpacks;

import com.lodestar.core.net.CoreNet;
import com.lodestar.core.util.FlightUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jetpack flight. Players move on their own client, so the client applies the thrust to its own player while the
 * server, told which keys are held, burns the fuel, cancels fall damage, keeps the anti-fly check quiet and tells
 * everyone nearby to draw the exhaust.
 */
public final class JetpackLogic {
    public static final int JUMP = 1, SNEAK = 2, FWD = 4, BACK = 8, LEFT = 16, RIGHT = 32, SPRINT = 64;

    /** what each flying player last told the server it is holding */
    private static final Map<UUID, Integer> INPUT = new HashMap<>();
    /** players whose exhaust is currently on (server) */
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    private JetpackLogic() {}

    public enum Mode { OFF, THRUST, HOVER }

    public static ItemStack worn(Player p) {
        ItemStack s = p.getItemBySlot(EquipmentSlot.CHEST);
        return s.getItem() instanceof JetpackItem ? s : ItemStack.EMPTY;
    }

    /** What the jetpack does this tick for these keys. */
    public static Mode mode(Player p, ItemStack s, int input) {
        if (s.isEmpty() || !(s.getItem() instanceof JetpackItem j)) return Mode.OFF;
        if (p.getAbilities().flying || p.isSpectator() || p.isPassenger() || !JetpackItem.engineOn(s) || !j.hasPower(s)) return Mode.OFF;
        if ((input & JUMP) != 0) return Mode.THRUST;
        if (JetpackItem.hoverOn(s) && j.canHover(s) && !p.onGround() && !p.isInWater()) return Mode.HOVER;
        return Mode.OFF;
    }

    /** Applies thrust to a player's motion. Runs on the owning client (and in tests on the server). */
    public static void move(Player p, ItemStack s, int input) {
        Mode m = mode(p, s, input);
        if (m == Mode.OFF) return;
        JetpackItem j = (JetpackItem) s.getItem();
        Vec3 v = p.getDeltaMovement();
        double vy = v.y;
        if (p.isFallFlying() && m == Mode.THRUST && j.tier.ordinal() >= JetpackTier.STEEL.ordinal()) {
            // with an elytra the jets push along the look direction instead
            Vec3 look = p.getLookAngle();
            p.setDeltaMovement(v.add(look.x * 0.06 + (look.x * 1.5 - v.x) * 0.1, look.y * 0.06 + (look.y * 1.5 - v.y) * 0.1, look.z * 0.06 + (look.z * 1.5 - v.z) * 0.1));
            return;
        }
        if (m == Mode.THRUST) {
            double a = j.accel(s);
            vy = vy < 0 ? vy + a * 2 : Math.min(j.climb(s), vy + a);
            if ((input & SNEAK) != 0) vy = Math.min(vy, 0.15);
        } else {
            double floor = (input & SNEAK) != 0 ? -0.35 : (j.tier.hover ? -0.02 : -0.08) / (1 + JetpackItem.level(s, JetpackItem.Mod.STABILIZER));
            if (vy < floor) vy = Math.min(floor, vy + 0.2);
        }
        float fwd = ((input & FWD) != 0 ? 1 : 0) - ((input & BACK) != 0 ? 1 : 0);
        float strafe = ((input & LEFT) != 0 ? 1 : 0) - ((input & RIGHT) != 0 ? 1 : 0);
        double sx = v.x, sz = v.z;
        if (fwd != 0 || strafe != 0) {
            double push = j.tier.side * ((input & SPRINT) != 0 && j.tier.ordinal() >= JetpackTier.DURALUMIN.ordinal() ? 1.4 : 1.0);
            float yaw = p.getYRot() * ((float) Math.PI / 180f);
            double len = Math.sqrt(fwd * fwd + strafe * strafe);
            double fx = (strafe * Math.cos(yaw) - fwd * Math.sin(yaw)) / len;
            double fz = (fwd * Math.cos(yaw) + strafe * Math.sin(yaw)) / len;
            sx += fx * push;
            sz += fz * push;
            double h = Math.sqrt(sx * sx + sz * sz), max = j.tier.sideMax * ((input & SPRINT) != 0 ? 1.25 : 1.0);
            if (h > max) {
                sx *= max / h;
                sz *= max / h;
            }
        }
        p.setDeltaMovement(sx, vy, sz);
        p.fallDistance = 0;
        p.resetFallDistance();
    }

    // ------------------------------------------------------------------ server

    public static void setInput(ServerPlayer p, int input) {
        INPUT.put(p.getUUID(), input);
    }

    public static void forget(UUID id) {
        INPUT.remove(id);
        ACTIVE.remove(id);
    }

    public static int input(Player p) {
        return INPUT.getOrDefault(p.getUUID(), 0);
    }

    /** Server side of one tick of flight: fuel, fall damage, the anti-fly counter and exhaust effects. */
    public static void serverTick(ServerPlayer p) {
        ItemStack s = worn(p);
        int input = input(p);
        Mode m = mode(p, s, input);
        if (m != Mode.OFF) {
            JetpackItem j = (JetpackItem) s.getItem();
            if (!j.consume(s, m == Mode.THRUST ? 1.0 : 0.4)) m = Mode.OFF;
            p.fallDistance = 0;
            FlightUtil.allowFloating(p);
            if (p.tickCount % 8 == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), m == Mode.THRUST ? SoundEvents.FIRECHARGE_USE : SoundEvents.BLAZE_BURN,
                        SoundSource.PLAYERS, m == Mode.THRUST ? 0.12f : 0.05f, j.tier.fuel ? 0.6f : 1.4f);
            }
            if (j.charge(s) < 0.1f && p.tickCount % 40 == 0) {
                p.displayClientMessage(Component.translatable("msg.ls_jetpacks.low").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
        int state = m.ordinal();
        Integer before = ACTIVE.get(p.getUUID());
        if (before == null || before != state) {
            if (state == 0) ACTIVE.remove(p.getUUID());
            else ACTIVE.put(p.getUUID(), state);
            CompoundTag t = new CompoundTag();
            t.putInt("Id", p.getId());
            t.putInt("Mode", state);
            CoreNet.toTracking(p.level().getChunkAt(p.blockPosition()), "ls_jetpacks:fx", t);
            CoreNet.toPlayer(p, "ls_jetpacks:fx", t);
        }
    }

    /** Engine (0) or hover (1) switch pressed. */
    public static void toggle(ServerPlayer p, int which) {
        ItemStack s = worn(p);
        if (!(s.getItem() instanceof JetpackItem j)) return;
        CompoundTag t = s.getOrCreateTag();
        if (which == 0) {
            boolean off = !t.getBoolean(JetpackItem.ENGINE_OFF);
            t.putBoolean(JetpackItem.ENGINE_OFF, off);
            p.displayClientMessage(Component.translatable(off ? "tooltip.ls_jetpacks.engine_off" : "tooltip.ls_jetpacks.engine_on"), true);
        } else if (j.canHover(s)) {
            boolean on = !t.getBoolean(JetpackItem.HOVER);
            t.putBoolean(JetpackItem.HOVER, on);
            p.displayClientMessage(Component.translatable(on ? "tooltip.ls_jetpacks.hover_on" : "tooltip.ls_jetpacks.hover_off"), true);
        } else {
            p.displayClientMessage(Component.translatable("msg.ls_jetpacks.no_hover"), true);
        }
    }
}
