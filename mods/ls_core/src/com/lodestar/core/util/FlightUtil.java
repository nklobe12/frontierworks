package com.lodestar.core.util;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * For gear that lets players fly without creative flight (jetpacks, gliders, mechs): vanilla kicks a player who
 * floats for 4 seconds when allow-flight is off. Calling {@link #allowFloating} each tick while the gear is holding
 * the player up resets that counter.
 */
public final class FlightUtil {
    private static final Logger LOG = LogUtils.getLogger();
    private static Field aboveGround;
    private static boolean failed;

    private FlightUtil() {}

    public static void allowFloating(ServerPlayer player) {
        if (failed || player.connection == null) return;
        try {
            if (aboveGround == null) aboveGround = ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class, "f_9737_");
            aboveGround.setInt(player.connection, 0);
        } catch (Throwable t) {
            failed = true;
            LOG.warn("Could not reset the floating counter; flight gear may trip the server's anti-fly check", t);
        }
    }
}
