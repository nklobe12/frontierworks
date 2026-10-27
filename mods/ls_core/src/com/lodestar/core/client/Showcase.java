package com.lodestar.core.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CI showcase (-Dlodestar.showcase=true with --quickPlaySingleplayer): builds scenes in the integrated server (walls
 * of every block, creatures, dimensions) and opens screens (inventory with Lens, recipe viewer, machine GUIs, and any
 * screen a mod registers with {@link #screen}), saves a screenshot of each to screenshots/ls_*.png, then quits.
 */
public final class Showcase {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<Step> steps = new ArrayList<>();
    private static final Map<String, Supplier<Screen>> SCREENS = new LinkedHashMap<>();
    private static final List<String> GUI_BLOCKS = new ArrayList<>();
    private static int wait = -1;
    private static int renderWait;
    private static boolean started;

    private record Step(int waitAfter, Consumer<Minecraft> action) {}

    private Showcase() {}

    public static boolean enabled() {
        return "true".equals(System.getProperty("lodestar.showcase"));
    }

    /** A screen to photograph (client side; e.g. a guide book page). */
    public static void screen(String name, Supplier<Screen> screen) {
        SCREENS.put(name, screen);
    }

    /** A block whose GUI should be photographed: it is placed beside the player and used. */
    public static void guiBlock(String blockId) {
        GUI_BLOCKS.add(blockId);
    }

    public static void init() {
        if (!enabled()) return;
        MinecraftForge.EVENT_BUS.addListener(Showcase::onTick);
    }

    private static void server(Minecraft mc, Consumer<MinecraftServer> work) {
        MinecraftServer srv = mc.getSingleplayerServer();
        if (srv != null) srv.execute(() -> {
            try {
                work.accept(srv);
            } catch (Throwable t) {
                LOG.error("LSSHOWCASE server step failed", t);
            }
        });
    }

    private static ServerPlayer player(MinecraftServer srv) {
        return srv.getPlayerList().getPlayers().isEmpty() ? null : srv.getPlayerList().getPlayers().get(0);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "ls_" + name + ".png", mc.getMainRenderTarget(), msg -> LOG.info("LSSHOWCASE shot {}", name));
    }

    private static void place(ServerPlayer p, ServerLevel lvl, double x, double y, double z, float yaw, float pitch) {
        BlockPos feet = BlockPos.containing(x, y, z);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos b = feet.offset(dx, -1, dz);
                if (lvl.isEmptyBlock(b)) lvl.setBlock(b, Blocks.BARRIER.defaultBlockState(), 2);
            }
        p.teleportTo(lvl, x, feet.getY(), z, yaw, pitch);
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
                started = true;
                wait = 100;
                plan();
                LOG.info("LSSHOWCASE world loaded, {} steps", steps.size());
            }
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (renderWait > 0) {
            renderWait--;
            if (!viewCompiled(mc)) return;
            renderWait = 0;
            wait = 40;
            return;
        }
        if (steps.isEmpty()) {
            if (wait == 0) {
                LOG.info("LSSHOWCASE DONE");
                wait = -1;
                mc.stop();
            }
            return;
        }
        Step s = steps.remove(0);
        try {
            s.action.accept(mc);
        } catch (Throwable t) {
            LOG.error("LSSHOWCASE step failed", t);
        }
        wait = s.waitAfter;
        renderWait = s.waitAfter > 100 ? 1200 : 0;
    }

    private static boolean viewCompiled(Minecraft mc) {
        if (mc.player == null || mc.levelRenderer == null) return true;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        for (int d = 0; d <= 32; d += 8) {
            if (!mc.levelRenderer.isChunkCompiled(BlockPos.containing(eye.add(look.scale(d))))) return false;
        }
        return true;
    }

    private static double[] viewpoint(ServerLevel lvl) {
        double[] best = {64, 150, 64};
        int bestScore = -1;
        for (int gx = -2; gx <= 2; gx++) {
            for (int gz = -2; gz <= 2; gz++) {
                int x = 64 + gx * 48, z = 64 + gz * 48;
                lvl.getChunk(x >> 4, z >> 4);
                if (lvl.dimensionType().hasCeiling()) {
                    int run = 0;
                    int top = lvl.getMinBuildHeight() + lvl.dimensionType().logicalHeight() - 4;
                    for (int y = top; y > lvl.getMinBuildHeight() + 4; y--) {
                        BlockPos bp = new BlockPos(x, y, z);
                        if (lvl.isEmptyBlock(bp)) run++;
                        else {
                            if (run > bestScore && lvl.getBlockState(bp).isSolidRender(lvl, bp)) {
                                bestScore = run;
                                best = new double[]{x, y + 1 + Math.min(4, run / 3), z};
                            }
                            run = 0;
                        }
                    }
                } else {
                    int h = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (h > lvl.getMinBuildHeight() + 8 && h > bestScore) {
                        bestScore = h;
                        best = new double[]{x, h + 6, z};
                    }
                }
            }
        }
        return best;
    }

    private static void add(int waitAfter, Consumer<Minecraft> action) {
        steps.add(new Step(waitAfter, action));
    }

    private static void plan() {
        add(5, mc -> {
            mc.options.hideGui = true;
            server(mc, srv -> {
                ServerPlayer p = player(srv);
                if (p == null) return;
                p.setGameMode(GameType.CREATIVE);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 3600, 0, false, false));
                srv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, srv);
                srv.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, srv);
                srv.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, srv);
                srv.overworld().setDayTime(6000);
                srv.overworld().setWeatherParameters(6000, 0, false, false);
            });
        });
        // 1. walls of blocks
        List<Block> blocks = new ArrayList<>();
        for (Map.Entry<ResourceKey<Block>, Block> e : ForgeRegistries.BLOCKS.getEntries()) {
            if (!e.getKey().location().getNamespace().startsWith("ls_")) continue;
            Block b = e.getValue();
            if (b instanceof LiquidBlock || b.defaultBlockState().isAir()) continue;
            blocks.add(b);
        }
        int perPage = 16 * 8;
        int pages = (blocks.size() + perPage - 1) / perPage;
        for (int page = 0; page < pages; page++) {
            final int pg = page;
            final List<Block> slice = blocks.subList(page * perPage, Math.min(blocks.size(), (page + 1) * perPage));
            add(400, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.overworld();
                BlockPos sp = lvl.getSharedSpawnPos();
                int x0 = sp.getX() - 8, y0 = 236, z0 = sp.getZ() - 30;
                for (int x = -2; x < 18; x++)
                    for (int y = -2; y < 10; y++) lvl.setBlock(new BlockPos(x0 + x, y0 + y, z0), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2 | 16);
                for (int i = 0; i < slice.size(); i++) {
                    BlockPos pos = new BlockPos(x0 + (i % 16), y0 + 7 - (i / 16), z0 + 1);
                    try {
                        lvl.setBlock(pos, slice.get(i).defaultBlockState(), 2 | 16);
                    } catch (Throwable t) {
                        LOG.warn("LSSHOWCASE could not place {}", slice.get(i), t);
                    }
                }
                ServerPlayer p = player(srv);
                if (p != null) place(p, lvl, x0 + 7.5, y0 + 2.0, z0 + 11.5, 180f, 8f);
            }));
            add(10, mc -> shot(mc, "blocks_" + (pg + 1)));
        }
        // 2. creatures
        List<EntityType<?>> mobs = new ArrayList<>();
        for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> e : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            if (e.getKey().location().getNamespace().startsWith("ls_")) mobs.add(e.getValue());
        }
        add(300, mc -> server(mc, srv -> {
            ServerLevel lvl = srv.overworld();
            BlockPos sp = lvl.getSharedSpawnPos();
            int x0 = sp.getX() + 40, y0 = 236, z0 = sp.getZ() - 30;
            for (int x = -4; x < 44; x++)
                for (int z = -6; z < 20; z++) lvl.setBlock(new BlockPos(x0 + x, y0 - 1, z0 + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
            int n = 0;
            for (EntityType<?> t : mobs) {
                Entity e = t.create(lvl);
                if (!(e instanceof Mob mob)) continue;
                boolean big = mob.getBbWidth() > 1.5f;
                mob.moveTo(x0 + 2 + n * 3.5 + (big ? 2 : 0), y0, z0 + (big ? -2 : 0), 0f, 0f);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                lvl.addFreshEntity(mob);
                n += big ? 2 : 1;
            }
            ServerPlayer p = player(srv);
            if (p != null) place(p, lvl, x0 + 18.0, y0 + 3.0, z0 + 16.0, 180f, 10f);
        }));
        add(10, mc -> shot(mc, "creatures"));
        // 3. dimensions added by the pack
        List<ResourceKey<net.minecraft.world.level.Level>> dims = new ArrayList<>();
        Minecraft mc0 = Minecraft.getInstance();
        if (mc0.getSingleplayerServer() != null) {
            for (ResourceKey<net.minecraft.world.level.Level> k : mc0.getSingleplayerServer().levelKeys()) {
                if (k.location().getNamespace().startsWith("ls_")) dims.add(k);
            }
        }
        for (ResourceKey<net.minecraft.world.level.Level> k : dims) {
            add(300, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.getLevel(k);
                ServerPlayer p = player(srv);
                if (lvl == null || p == null) return;
                double[] v = viewpoint(lvl);
                place(p, lvl, v[0] + 0.5, v[1], v[2] + 0.5, 45f, 10f);
            }));
            add(10, mc -> shot(mc, "dimension_" + k.location().getPath()));
        }
        // 4. screens: back to a quiet spot, GUI on
        add(120, mc -> {
            mc.options.hideGui = false;
            server(mc, srv -> {
                ServerPlayer p = player(srv);
                BlockPos sp = srv.overworld().getSharedSpawnPos();
                if (p != null) place(p, srv.overworld(), sp.getX() + 0.5, 240, sp.getZ() + 0.5, 0f, 0f);
            });
        });
        add(20, mc -> mc.setScreen(new InventoryScreen(mc.player)));
        add(5, mc -> shot(mc, "gui_inventory"));
        add(20, mc -> {
            mc.setScreen(new InventoryScreen(mc.player));
            ItemStack target = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_machina", "pulverizer")));
            LensBridge.recipes.accept(target);
        });
        add(5, mc -> shot(mc, "gui_lens_recipes"));
        add(20, mc -> {
            mc.setScreen(new InventoryScreen(mc.player));
            LensBridge.uses.accept(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        });
        add(5, mc -> shot(mc, "gui_lens_uses"));
        add(5, mc -> mc.setScreen(null));
        for (String id : GUI_BLOCKS) {
            add(30, mc -> server(mc, srv -> {
                ServerPlayer p = player(srv);
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
                if (p == null || b == null || b == Blocks.AIR) return;
                ServerLevel lvl = p.serverLevel();
                BlockPos pos = p.blockPosition().relative(Direction.NORTH, 2);
                lvl.setBlock(pos, b.defaultBlockState(), 3);
                BlockState st = lvl.getBlockState(pos);
                st.use(lvl, p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
            }));
            add(5, mc -> shot(mc, "gui_" + id.replace(':', '_')));
            add(5, mc -> {
                if (mc.player != null) mc.player.closeContainer();
                mc.setScreen(null);
            });
        }
        for (Map.Entry<String, Supplier<Screen>> e : SCREENS.entrySet()) {
            add(30, mc -> mc.setScreen(e.getValue().get()));
            add(5, mc -> shot(mc, "screen_" + e.getKey()));
        }
        add(40, mc -> mc.setScreen(null));
    }
}
