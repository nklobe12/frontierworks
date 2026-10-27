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
    private static boolean started;
    private static Button pendingPress;
    private static int pressDelay;

    private record Step(int waitAfter, Consumer<Minecraft> action) {}

    private ShowcaseClient() {}

    public static void init() {
        if (!"true".equals(System.getProperty("fwcore.showcase"))) return;
        MinecraftForge.EVENT_BUS.addListener(ShowcaseClient::onTick);
        MinecraftForge.EVENT_BUS.addListener(ShowcaseClient::onScreen);
    }

    /** Auto-confirm "experimental settings" / backup prompts when loading the test world. */
    private static void onScreen(ScreenEvent.Init.Post event) {
        Screen s = event.getScreen();
        if (s instanceof ConfirmScreen || s.getClass().getSimpleName().contains("Backup") || s.getClass().getSimpleName().contains("Experimental")) {
            for (var l : event.getListenersList()) {
                if (l instanceof Button b) {
                    pendingPress = b;
                    pressDelay = 10;
                    LOG.info("FWSHOWCASE auto-confirming {}", s.getClass().getSimpleName());
                    return;
                }
            }
        }
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
        Screenshot.grab(mc.gameDirectory, "fw_" + name + ".png", mc.getMainRenderTarget(), msg -> LOG.info("FWSHOWCASE shot {}", name));
    }

    private static void command(MinecraftServer srv, String cmd) {
        srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (pendingPress != null && --pressDelay <= 0) {
            Button b = pendingPress;
            pendingPress = null;
            b.onPress();
        }
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
            add(140, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.overworld();
                int x0 = 20000 + pg * 64, y0 = 200, z0 = 20000;
                for (int x = -2; x < 18; x++)
                    for (int y = -2; y < 10; y++) {
                        lvl.setBlock(new BlockPos(x0 + x, y0 + y, z0), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2 | 16);
                    }
                for (int i = 0; i < slice.size(); i++) {
                    BlockPos pos = new BlockPos(x0 + (i % 16), y0 + 7 - (i / 16), z0 + 1);
                    lvl.setBlock(pos, slice.get(i).defaultBlockState(), 2 | 16);
                }
                ServerPlayer p = player(srv);
                if (p != null) p.teleportTo(lvl, x0 + 7.5, y0 + 2.0, z0 + 11.5, 180f, 8f);
            }));
            add(10, mc -> shot(mc, "blocks_" + (pg + 1)));
        }
        // 2. creatures
        List<EntityType<?>> mobs = new ArrayList<>();
        for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> e : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            if (e.getKey().location().getNamespace().startsWith("fw_")) mobs.add(e.getValue());
        }
        add(160, mc -> server(mc, srv -> {
            ServerLevel lvl = srv.overworld();
            int x0 = 21000, y0 = 200, z0 = 21000;
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
            if (p != null) p.teleportTo(lvl, x0 + 18.0, y0 + 3.0, z0 + 16.0, 180f, 10f);
        }));
        add(10, mc -> shot(mc, "creatures"));
        add(60, mc -> server(mc, srv -> {
            ServerPlayer p = player(srv);
            if (p != null) p.teleportTo(srv.overworld(), 21000 + 8.0, 202.0, 21000 + 9.0, 180f, 12f);
        }));
        add(10, mc -> shot(mc, "creatures_close"));
        // 3. structures from their templates on a flat platform
        String[] templates = {"fw_towers:wizard_tower/start/0", "fw_temples:sun_pyramid/start/0", "fw_outposts:research_station/start/0",
                "fw_ruins:ruined_house/start/0", "fw_camps:bandit_camp/start/0", "fw_waystones:waystone_shrine/start/0", "fw_skyislands:sky_grove/start/0",
                "fw_meteors:crater_medium/start/0", "fw_bunkers:bunker/start/0", "fw_temples:frost_shrine/start/0"};
        for (int i = 0; i < templates.length; i++) {
            final String id = templates[i];
            final int idx = i;
            add(160, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.overworld();
                Optional<StructureTemplate> tpl = srv.getStructureManager().get(new ResourceLocation(id));
                if (tpl.isEmpty()) {
                    LOG.warn("FWSHOWCASE missing template {}", id);
                    return;
                }
                Vec3i size = tpl.get().getSize();
                int x0 = 22000 + idx * 128, y0 = 120, z0 = 22000;
                for (int x = -20; x < size.getX() + 20; x++)
                    for (int z = -20; z < size.getZ() + 40; z++) {
                        lvl.setBlock(new BlockPos(x0 + x, y0 - 1, z0 + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
                        for (int y = 0; y < size.getY() + 4; y++) lvl.setBlock(new BlockPos(x0 + x, y0 + y, z0 + z), Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                command(srv, "place template " + id + " " + x0 + " " + y0 + " " + z0);
                int span = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
                ServerPlayer p = player(srv);
                if (p != null) p.teleportTo(lvl, x0 + size.getX() / 2.0, y0 + size.getY() * 0.55 + 2, z0 + size.getZ() + span * 0.9 + 4, 180f, 12f);
            }));
            add(10, mc -> shot(mc, "structure_" + id.substring(3, id.indexOf('/')).replace(':', '_')));
        }
        // 4. other dimensions
        String[][] dims = {{"fw_moon:moon", "20"}, {"fw_mars:mars", "20"}, {"fw_skylands:skylands", "-10"}, {"fw_depths:depths", "0"}};
        for (String[] d : dims) {
            add(400, mc -> server(mc, srv -> {
                ServerLevel lvl = srv.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(d[0])));
                ServerPlayer p = player(srv);
                if (lvl == null || p == null) return;
                double y;
                if (lvl.dimensionType().hasCeiling()) {
                    BlockPos safe = com.frontierworks.core.util.TeleportUtil.findSafe(lvl, 64, 64, null);
                    y = safe.getY() + 1;
                } else if (d[0].contains("skylands")) {
                    y = 150;
                } else {
                    lvl.getChunk(4, 4);
                    y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, 64, 64) + 6;
                }
                p.teleportTo(lvl, 64.5, y, 64.5, 45f, Float.parseFloat(d[1]));
            }));
            add(10, mc -> shot(mc, "dimension_" + d[0].substring(d[0].indexOf(':') + 1)));
        }
        add(40, mc -> server(mc, srv -> {
            ServerPlayer p = player(srv);
            if (p != null) p.teleportTo(srv.overworld(), 20000.5, 220, 20000.5, 0f, 0f);
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
