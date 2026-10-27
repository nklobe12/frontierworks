package com.frontierworks.core.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CI self-test, enabled with -Dfwcore.selftest=server. Exercises every Frontierworks block (placed and
 * ticked), structure (placed with /place), dimension (chunks generated) and entity (spawned and
 * ticked), logs "FWSELFTEST" lines and stops the server.
 */
public final class SelfTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<Runnable> steps = new ArrayList<>();
    private static int wait;
    private static int failures;
    private static final List<Entity> spawned = new ArrayList<>();
    private static final List<BlockPos> placed = new ArrayList<>();
    private static MinecraftServer server;

    private SelfTest() {}

    public static boolean enabled() {
        return "server".equals(System.getProperty("fwcore.selftest"));
    }

    public static void init() {
        if (!enabled()) return;
        MinecraftForge.EVENT_BUS.addListener(SelfTest::onStarted);
        MinecraftForge.EVENT_BUS.addListener(SelfTest::onTick);
    }

    private static void fail(String what, Throwable t) {
        failures++;
        LOG.error("FWSELFTEST FAIL {}", what, t);
    }

    private static void onStarted(ServerStartedEvent event) {
        server = event.getServer();
        LOG.info("FWSELFTEST START");
        final ServerLevel overworld = server.overworld();
        final List<JsonObject> specs = readSpecs();

        // 1. blocks
        steps.add(() -> {
            int i = 0;
            int ok = 0;
            for (Map.Entry<ResourceKey<Block>, Block> e : ForgeRegistries.BLOCKS.getEntries()) {
                ResourceLocation id = e.getKey().location();
                if (!id.getNamespace().startsWith("fw_")) continue;
                BlockPos pos = new BlockPos(4000 + (i % 32) * 2, 250, 4000 + (i / 32) * 2);
                i++;
                try {
                    overworld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    BlockState st = e.getValue().defaultBlockState();
                    overworld.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                    overworld.setBlock(pos, st, 3);
                    placed.add(pos);
                    ok++;
                } catch (Throwable t) {
                    fail("block " + id, t);
                }
            }
            LOG.info("FWSELFTEST blocks placed={}", ok);
            wait = 60;
        });
        steps.add(() -> {
            for (BlockPos p : placed) {
                try {
                    overworld.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                } catch (Throwable t) {
                    fail("remove block at " + p, t);
                }
            }
            LOG.info("FWSELFTEST blocks removed");
        });

        // 2. dimensions
        steps.add(() -> {
            for (ResourceKey<Level> key : server.levelKeys()) {
                if (!key.location().getNamespace().startsWith("fw_")) continue;
                ServerLevel lvl = server.getLevel(key);
                if (lvl == null) continue;
                try {
                    for (int cx = -1; cx <= 1; cx++)
                        for (int cz = -1; cz <= 1; cz++)
                            lvl.getChunk(cx, cz);
                    LOG.info("FWSELFTEST dimension ok {}", key.location());
                } catch (Throwable t) {
                    fail("dimension " + key.location(), t);
                }
            }
        });

        // 3. structures
        steps.add(() -> {
            List<ResourceLocation> ids = new ArrayList<>();
            for (Map.Entry<ResourceKey<Structure>, Structure> e : server.registryAccess().registryOrThrow(Registries.STRUCTURE).entrySet()) {
                if (e.getKey().location().getNamespace().startsWith("fw_")) ids.add(e.getKey().location());
            }
            int n = 0;
            for (ResourceLocation id : ids) {
                String dim = dimFor(specs, id.toString());
                int x = 8000 + n * 160;
                n++;
                String cmd = "execute in " + dim + " run place structure " + id + " " + x + " 120 8000";
                try {
                    int r = server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
                    if (r <= 0) {
                        failures++;
                        LOG.error("FWSELFTEST FAIL structure {} (command returned {})", id, r);
                    } else {
                        LOG.info("FWSELFTEST structure ok {}", id);
                    }
                } catch (Throwable t) {
                    fail("structure " + id, t);
                }
            }
            wait = 20;
        });

        // 4. entities
        steps.add(() -> {
            int i = 0;
            for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> e : ForgeRegistries.ENTITY_TYPES.getEntries()) {
                ResourceLocation id = e.getKey().location();
                if (!id.getNamespace().startsWith("fw_")) continue;
                try {
                    Entity ent = e.getValue().create(overworld);
                    if (ent == null) {
                        LOG.info("FWSELFTEST entity skipped (not creatable) {}", id);
                        continue;
                    }
                    BlockPos pos = new BlockPos(6000 + i * 8, 200, 6000);
                    i++;
                    overworld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    overworld.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                    ent.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                    overworld.addFreshEntity(ent);
                    spawned.add(ent);
                    LOG.info("FWSELFTEST entity spawned {}", id);
                } catch (Throwable t) {
                    fail("entity " + id, t);
                }
            }
            wait = 100;
        });
        steps.add(() -> {
            for (Entity e : spawned) {
                try {
                    e.discard();
                } catch (Throwable t) {
                    fail("discard entity", t);
                }
            }
            LOG.info("FWSELFTEST entities ticked");
        });

        // 5. done
        steps.add(() -> {
            LOG.info("FWSELFTEST DONE failures={}", failures);
            server.halt(false);
        });
        wait = 20;
    }

    private static List<JsonObject> readSpecs() {
        List<JsonObject> out = new ArrayList<>();
        try {
            Map<ResourceLocation, Resource> res = server.getResourceManager().listResources("fw_selftest", p -> p.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> e : res.entrySet()) {
                try (Reader r = e.getValue().openAsReader()) {
                    JsonElement el = JsonParser.parseReader(r);
                    if (el.isJsonObject()) out.add(el.getAsJsonObject());
                }
            }
        } catch (Throwable t) {
            fail("reading selftest specs", t);
        }
        return out;
    }

    private static String dimFor(List<JsonObject> specs, String structure) {
        for (JsonObject o : specs) {
            if (!o.has("structures")) continue;
            JsonArray arr = o.getAsJsonArray("structures");
            for (JsonElement el : arr) {
                JsonObject s = el.getAsJsonObject();
                if (structure.equals(s.get("id").getAsString())) {
                    return s.has("dim") ? s.get("dim").getAsString() : "minecraft:overworld";
                }
            }
        }
        return "minecraft:overworld";
    }

    private static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) return;
        if (wait > 0) {
            wait--;
            return;
        }
        if (steps.isEmpty()) return;
        Runnable r = steps.remove(0);
        try {
            r.run();
        } catch (Throwable t) {
            fail("step", t);
        }
    }
}
