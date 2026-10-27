package com.frontierworks.core.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * One shared client->server channel for every Frontierworks mod. Mods register named actions
 * (e.g. "fw_jetpack:thrust") and the client sends them with an int payload.
 */
public final class CoreNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("fw_core", "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static final Map<String, BiConsumer<ServerPlayer, Integer>> ACTIONS = new ConcurrentHashMap<>();

    private CoreNet() {}

    public static void init() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Action::encode).decoder(Action::decode).consumerMainThread(Action::handle).add();
    }

    public static void register(String action, BiConsumer<ServerPlayer, Integer> handler) {
        ACTIONS.put(action, handler);
    }

    /** Client side: send an action to the server. */
    public static void send(String action, int data) {
        CHANNEL.sendToServer(new Action(action, data));
    }

    public static class Action {
        final String id;
        final int data;

        public Action(String id, int data) {
            this.id = id;
            this.data = data;
        }

        static void encode(Action a, FriendlyByteBuf buf) {
            buf.writeUtf(a.id, 128);
            buf.writeVarInt(a.data);
        }

        static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readUtf(128), buf.readVarInt());
        }

        static void handle(Action a, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            BiConsumer<ServerPlayer, Integer> h = ACTIONS.get(a.id);
            if (player != null && h != null) {
                h.accept(player, a.data);
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
