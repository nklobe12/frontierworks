package com.frontierworks.guide.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * CI showcase (-Dfwcore.showcase=true with --quickPlaySingleplayer): builds scenes in the integrated server
 * (block walls, mobs, structures, other dimensions, guide pages) and saves a screenshot of each, then quits.
 */
public final class ShowcaseClient {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<Step> steps = new ArrayList<>();
    private static int wait = -1;
    private static int renderWait;   // >0: after the fixed wait, also wait (up to this many ticks) for chunks in view to be compiled
    private static boolean started;

    private record Step(int waitAfter, Consumer<Minecraft> action) {}

    private ShowcaseClient() {}

    public static void init() {
        if (!"true".equals(System.getProperty("fwcore.showcase"))) return;
        MinecraftForge.EVENT_BUS.addListener(ShowcaseClient::onTick);
    }

    private static void server(Minecraft mc, Consumer<MinecraftServer> work) {
        MinecraftServer srv = mc.getSingleplayerServer();
        if (srv != null) srv.execute(() -> {
            try {
                work.accept(srv);
            } catch (Throwable t) {
                LOG.error("FWSHOWCASE server step failed", t);
            }
        });
    }

    private static ServerPlayer player(MinecraftServer srv) {
        return srv.getPlayerList().getPlayers().isEmpty() ? null : srv.getPlayerList().getPlayers().get(0);
    }

    private static void shot(Minecraft mc, String name) {
        if (mc.player != null) LOG.info("FWSHOWCASE taking {} at {} {} {} in {}", name, (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ(),
                mc.player.level().dimension().location());
        Screenshot.grab(mc.gameDirectory, "fw_" + name + ".png", mc.getMainRenderTarget(), msg -> LOG.info("FWSHOWCASE shot {}", name));
    }

    /** Teleports the player and puts an invisible barrier pad under their feet so the camera stays exactly there. */
    private static void place(ServerPlayer p, ServerLevel lvl, double x, double y, double z, float yaw, float pitch) {
        BlockPos feet = BlockPos.containing(x, y, z);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos b = feet.offset(dx, -1, dz);
                if (lvl.isEmptyBlock(b)) lvl.setBlock(b, Blocks.BARRIER.defaultBlockState(), 2);
            }
        p.teleportTo(lvl, x, feet.getY(), z, yaw, pitch);
    }

    private static void command(MinecraftServer srv, String cmd) {
        srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
                started = true;
                wait = 100;
                plan();
                LOG.info("FWSHOWCASE world loaded, {} steps", steps.size());
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
                LOG.info("FWSHOWCASE DONE");
                wait = -1;
                mc.stop();
            }
            return;
        }
        Step s = steps.remove(0);
        try {
            s.action.accept(mc);
        } catch (Throwable t) {
            LOG.error("FWSHOWCASE step failed", t);
        }
        wait = s.waitAfter;
        renderWait = s.waitAfter > 100 ? 1200 : 0;
    }

    /** A good camera spot: the biggest cavern for ceiling worlds, otherwise just above the highest ground (floating islands included). */
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
                        if (lvl.isEmptyBlock(bp)) {
                            run++;
                        } else {
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

    /** True once the chunks around the player and along the view direction have been meshed. */
    private static boolean viewCompiled(Minecraft mc) {
        if (mc.player == null || mc.levelRenderer == null) return true;
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
        for (int d = 0; d <= 32; d += 8) {
            BlockPos p = BlockPos.containing(eye.add(look.scale(d)));
            if (!mc.levelRenderer.isChunkCompiled(p)) return false;
        }
        return true;
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
            if (!e.getKey().location().getNamespace().startsWith("fw_")) continue;
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
                BlockPos sp = srv.overworld().getSharedSpawnPos();
                int x0 = sp.getX() - 8, y0 = 236, z0 = sp.getZ() - 30;
                ServerPlayer early = player(srv);
                LOG.info("FWSHOWCASE blocks page {}: {} blocks, player {}", pg + 1, slice.size(), early == null ? "missing" : early.blockPosition());
                if (early != null) place(early, lvl, x0 + 7.5, y0 + 2.0, z0 + 11.5, 180f, 8f);
                for (int x = -2; x < 18; x++)
                    for (int y = -2; y < 10; y++) {
                        lvl.setBlock(new BlockPos(x0 + x, y0 + y, z0), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2 | 16);
                    }
                for (int i = 0; i < slice.size(); i++) {
                    BlockPos pos = new BlockPos(x0 + (i % 16), y0 + 7 - (i / 16), z0 + 1);
                    try {
                        lvl.setBlock(pos, slice.get(i).defaultBlockState(), 2 | 16);
                    } catch (Throwable t) {
                        LOG.warn("FWSHOWCASE could not place {}", slice.get(i), t);
                    }
                }
                LOG.info("FWSHOWCASE blocks page {} placed", pg + 1);
                ServerPlayer p = player(srv);
                if (p != null) place(p, lvl, x0 + 7.5, y0 + 2.0, z0 + 11.5, 180f, 8f);
            }));
            add(10, mc -> shot(mc, "blocks_" + (pg + 1)));
        }
        // 2. creatures
        List<EntityType<?>> mobs = new ArrayList<>();
        for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> e : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            if (e.getKey().location().getNamespace().startsWith("fw_")) mobs.add(e.getValue());
        }
        add(300, mc -> server(mc, srv -> {
            ServerLevel lvl = srv.overworld();
            BlockPos sp = srv.overworld().getSharedSpawnPos();
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
                mob.setYHeadRot(0f);
                mob.setYBodyRot(0f);
                lvl.addFreshEntity(mob);
                n += big ? 2 : 1;
            }
            ServerPlayer p = player(srv);
            if (p != null) place(p, lvl, x0 + 18.0, y0 + 3.0, z0 + 16.0, 180f, 10f);
        }));
        add(10, mc -> shot(mc, "creatures"));
        add(200, mc -> server(mc, srv -> {
            ServerPlayer p = player(srv);
            BlockPos sp = srv.overworld().getSharedSpawnPos();
            if (p != null) place(p, srv.overworld(), sp.getX() + 48.0, 238.0, sp.getZ() - 21.0, 180f, 12f);
        }));
        add(10, mc -> shot(mc, "creatures_close"));
        // 3. structures from their templates on a flat platform
        String[] templates = {"fw_towers:wizard_tower/start/0", "fw_temples:sun_pyramid/start/0", "fw_outposts:research_station/start/0",
                "fw_ruins:ruined_house/start/0", "fw_camps:bandit_camp/start/0", "fw_waystones:waystone_shrine/start/0", "fw_skyislands:sky_grove/start/0",
                "fw_meteors:crater_medium/start/0", "fw_bunkers:bunker/start/0", "fw_temples:frost_shrine/start/0"};
        for (int i = 0; i < templates.length; i++) {
            final String id = templates[i];
            final int idx = i;
            add(320, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.overworld();
                Optional<StructureTemplate> tpl = srv.getStructureManager().get(new ResourceLocation(id));
                if (tpl.isEmpty()) {
                    LOG.warn("FWSHOWCASE missing template {}", id);
                    return;
                }
                Vec3i size = tpl.get().getSize();
                BlockPos sp = srv.overworld().getSharedSpawnPos();
                int x0 = sp.getX() - 70 - size.getX() / 2, y0 = 190, z0 = sp.getZ() - 40 - size.getZ() / 2;
                for (int x = -24; x < 60; x++)
                    for (int z = -24; z < 70; z++) {
                        lvl.setBlock(new BlockPos(sp.getX() - 70 + x - 18, y0 - 1, sp.getZ() - 40 + z - 18), Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
                        for (int y = 0; y < 48; y++) lvl.setBlock(new BlockPos(sp.getX() - 70 + x - 18, y0 + y, sp.getZ() - 40 + z - 18), Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                command(srv, "place template " + id + " " + x0 + " " + y0 + " " + z0);
                int span = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
                ServerPlayer p = player(srv);
                if (p != null) place(p, lvl, x0 + size.getX() / 2.0, y0 + size.getY() * 0.55 + 2, z0 + size.getZ() + span * 0.9 + 4, 180f, 12f);
            }));
            add(10, mc -> shot(mc, "structure_" + id.substring(3, id.indexOf('/')).replace(':', '_')));
        }
        // 4. other dimensions
        String[][] dims = {{"fw_moon:moon", "20"}, {"fw_mars:mars", "20"}, {"fw_skylands:skylands", "-10"}, {"fw_depths:depths", "0"}};
        for (String[] d : dims) {
            add(300, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(d[0])));
                ServerPlayer p = player(srv);
                if (lvl == null || p == null) return;
                double[] v = viewpoint(lvl);
                place(p, lvl, v[0] + 0.5, v[1], v[2] + 0.5, 45f, Float.parseFloat(d[1]));
            }));
            add(10, mc -> shot(mc, "dimension_" + d[0].substring(d[0].indexOf(':') + 1)));
        }
        add(40, mc -> server(mc, srv -> {
            ServerPlayer p = player(srv);
            BlockPos sp = srv.overworld().getSharedSpawnPos();
            if (p != null) p.teleportTo(srv.overworld(), sp.getX() + 0.5, 240, sp.getZ() + 0.5, 0f, 0f);
        }));
        // 5. guide pages (item art)
        String[] guidePages = {"fw_tools", "fw_weapons", "fw_relics", "fw_food", "fw_metals", "fw_machines", "fw_mobs", "fw_rocketry"};
        for (String modid : guidePages) {
            add(30, mc -> mc.setScreen(new GuideScreen(GuideData.load(), modid)));
            add(5, mc -> shot(mc, "guide_" + modid.substring(3)));
        }
        add(40, mc -> mc.setScreen(null));
    }

    @SuppressWarnings("unused")
    private static boolean unused(Level l) {
        return l == null;
    }
}
