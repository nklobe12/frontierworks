package com.lodestar.core.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One shared channel for every Lodestar mod, so mods never write packet classes. Messages are named actions:
 * <ul>
 *   <li>client to server: {@link #register(String, BiConsumer)} an int handler, {@link #registerTag} a tag handler;
 *       send with {@link #send(String, int)} / {@link #sendTag(String, CompoundTag)}.</li>
 *   <li>server to client: {@link #registerClient(String, Consumer)} on the client, then {@link #toPlayer},
 *       {@link #toAll} or {@link #toTracking}.</li>
 * </ul>
 */
public final class CoreNet {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("ls_core", "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static final Map<String, BiConsumer<ServerPlayer, Integer>> ACTIONS = new ConcurrentHashMap<>();
    private static final Map<String, BiConsumer<ServerPlayer, CompoundTag>> TAG_ACTIONS = new ConcurrentHashMap<>();
    private static final Map<String, Consumer<CompoundTag>> CLIENT_ACTIONS = new ConcurrentHashMap<>();

    private CoreNet() {}

    public static void init() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Action::encode).decoder(Action::decode).consumerMainThread(Action::handle).add();
        CHANNEL.messageBuilder(TagAction.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TagAction::encode).decoder(TagAction::decode).consumerMainThread(TagAction::handle).add();
        CHANNEL.messageBuilder(ClientAction.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientAction::encode).decoder(ClientAction::decode).consumerMainThread(ClientAction::handle).add();
    }

    /** Server: handle a client action carrying an int. */
    public static void register(String action, BiConsumer<ServerPlayer, Integer> handler) {
        ACTIONS.put(action, handler);
    }

    /** Server: handle a client action carrying a compound tag. */
    public static void registerTag(String action, BiConsumer<ServerPlayer, CompoundTag> handler) {
        TAG_ACTIONS.put(action, handler);
    }

    /** Client: handle a server message. Runs on the client main thread. */
    public static void registerClient(String action, Consumer<CompoundTag> handler) {
        CLIENT_ACTIONS.put(action, handler);
    }

    /** Client side: send an action to the server. */
    public static void send(String action, int data) {
        CHANNEL.sendToServer(new Action(action, data));
    }

    public static void sendTag(String action, CompoundTag tag) {
        CHANNEL.sendToServer(new TagAction(action, tag));
    }

    public static void toPlayer(ServerPlayer player, String action, CompoundTag tag) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientAction(action, tag));
    }

    public static void toAll(String action, CompoundTag tag) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientAction(action, tag));
    }

    public static void toTracking(LevelChunk chunk, String action, CompoundTag tag) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientAction(action, tag));
    }

    public static void toDimension(ServerLevel level, String action, CompoundTag tag) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new ClientAction(action, tag));
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
            if (player != null && h != null) h.accept(player, a.data);
            ctx.get().setPacketHandled(true);
        }
    }

    public static class TagAction {
        final String id;
        final CompoundTag tag;

        public TagAction(String id, CompoundTag tag) {
            this.id = id;
            this.tag = tag;
        }

        static void encode(TagAction a, FriendlyByteBuf buf) {
            buf.writeUtf(a.id, 128);
            buf.writeNbt(a.tag);
        }

        static TagAction decode(FriendlyByteBuf buf) {
            String id = buf.readUtf(128);
            CompoundTag t = buf.readNbt();
            return new TagAction(id, t == null ? new CompoundTag() : t);
        }

        static void handle(TagAction a, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            BiConsumer<ServerPlayer, CompoundTag> h = TAG_ACTIONS.get(a.id);
            if (player != null && h != null) h.accept(player, a.tag);
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ClientAction {
        final String id;
        final CompoundTag tag;

        public ClientAction(String id, CompoundTag tag) {
            this.id = id;
            this.tag = tag;
        }

        static void encode(ClientAction a, FriendlyByteBuf buf) {
            buf.writeUtf(a.id, 128);
            buf.writeNbt(a.tag);
        }

        static ClientAction decode(FriendlyByteBuf buf) {
            String id = buf.readUtf(128);
            CompoundTag t = buf.readNbt();
            return new ClientAction(id, t == null ? new CompoundTag() : t);
        }

        static void handle(ClientAction a, Supplier<NetworkEvent.Context> ctx) {
            Consumer<CompoundTag> h = CLIENT_ACTIONS.get(a.id);
            if (h != null) h.accept(a.tag);
            ctx.get().setPacketHandled(true);
        }
    }
}
