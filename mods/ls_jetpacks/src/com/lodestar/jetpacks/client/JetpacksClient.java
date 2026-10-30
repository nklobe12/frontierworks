package com.lodestar.jetpacks.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.core.net.CoreNet;
import com.lodestar.jetpacks.JetpackItem;
import com.lodestar.jetpacks.JetpackLogic;
import com.lodestar.jetpacks.JetpackTier;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Keys, local flight, exhaust particles, the worn model and the flight HUD. */
public final class JetpacksClient {
    public static final KeyMapping ENGINE = new KeyMapping("key.ls_jetpacks.engine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.lodestar");
    public static final KeyMapping HOVER = new KeyMapping("key.ls_jetpacks.hover", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.lodestar");
    /** entity id -> mode ordinal for players whose exhaust is on */
    private static final Map<Integer, Integer> FX = new HashMap<>();
    private static int lastInput = -1, resend;
    private static int localMode;

    private JetpacksClient() {}

    public static void init(IEventBus bus) {
        for (JetpackTier t : JetpackTier.values()) RenderUtil.extraModel(new ResourceLocation("ls_jetpacks", "block/worn_" + t.id));
        bus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(ENGINE);
            e.register(HOVER);
        });
        bus.addListener((RegisterGuiOverlaysEvent e) -> e.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "jetpack", (gui, g, pt, w, h) -> hud(g, w, h)));
        bus.addListener(JetpacksClient::layers);
        CoreNet.registerClient("ls_jetpacks:fx", t -> {
            if (t.getInt("Mode") == 0) FX.remove(t.getInt("Id"));
            else FX.put(t.getInt("Id"), t.getInt("Mode"));
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.START && e.side.isClient() && e.player instanceof LocalPlayer lp) localTick(lp);
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) clientTick();
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> FX.clear());
    }

    private static int input(LocalPlayer p) {
        var o = Minecraft.getInstance().options;
        int i = 0;
        if (o.keyJump.isDown()) i |= JetpackLogic.JUMP;
        if (o.keyShift.isDown()) i |= JetpackLogic.SNEAK;
        if (o.keyUp.isDown()) i |= JetpackLogic.FWD;
        if (o.keyDown.isDown()) i |= JetpackLogic.BACK;
        if (o.keyLeft.isDown()) i |= JetpackLogic.LEFT;
        if (o.keyRight.isDown()) i |= JetpackLogic.RIGHT;
        if (o.keySprint.isDown() || p.isSprinting()) i |= JetpackLogic.SPRINT;
        return i;
    }

    private static void localTick(LocalPlayer p) {
        ItemStack s = JetpackLogic.worn(p);
        int in = Minecraft.getInstance().screen == null ? input(p) : 0;
        if (s.isEmpty()) in = 0;
        if (in != lastInput || ++resend >= 20) {
            CoreNet.send("ls_jetpacks:input", in);
            lastInput = in;
            resend = 0;
        }
        localMode = JetpackLogic.mode(p, s, in).ordinal();
        if (localMode != 0) JetpackLogic.move(p, s, in);
    }

    private static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        while (ENGINE.consumeClick()) if (!JetpackLogic.worn(mc.player).isEmpty()) CoreNet.send("ls_jetpacks:toggle", 0);
        while (HOVER.consumeClick()) if (!JetpackLogic.worn(mc.player).isEmpty()) CoreNet.send("ls_jetpacks:toggle", 1);
        for (Player p : mc.level.players()) {
            int mode = p == mc.player ? localMode : FX.getOrDefault(p.getId(), 0);
            if (mode != 0) exhaust(p, mode, JetpackLogic.worn(p));
        }
    }

    /** Flame and smoke from the two nozzles on the player's back. */
    private static void exhaust(Player p, int mode, ItemStack s) {
        if (!(s.getItem() instanceof JetpackItem j) || p.isInvisible()) return;
        var level = p.level();
        double yaw = Math.toRadians(p.yBodyRot);
        double bx = Math.sin(yaw) * 0.32, bz = -Math.cos(yaw) * 0.32;
        double rx = Math.cos(yaw) * 0.16, rz = Math.sin(yaw) * 0.16;
        double y = p.getY() + (p.isCrouching() ? 0.55 : 0.75);
        for (int side = -1; side <= 1; side += 2) {
            double x = p.getX() + bx + rx * side, z = p.getZ() + bz + rz * side;
            Vec3 v = p.getDeltaMovement();
            double down = mode == 1 ? -0.35 : -0.12;
            if (j.tier.fuel) {
                level.addParticle(ParticleTypes.FLAME, x, y, z, v.x * 0.5, down + v.y * 0.5, v.z * 0.5);
                if (p.getRandom().nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, x, y - 0.1, z, v.x * 0.3, down * 0.5, v.z * 0.3);
            } else {
                level.addParticle(mode == 1 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK, x, y, z, v.x * 0.5, down + v.y * 0.5, v.z * 0.5);
                if (mode == 1 && p.getRandom().nextInt(2) == 0) level.addParticle(ParticleTypes.END_ROD, x, y - 0.1, z, 0, down * 0.6, 0);
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ItemStack s = JetpackLogic.worn(mc.player);
        if (!(s.getItem() instanceof JetpackItem j)) return;
        var font = mc.font;
        int x = 6, y = h - 52;
        g.fill(x - 2, y - 2, x + 92, y + 44, 0x70000000);
        g.renderItem(s, x, y);
        float c = j.charge(s);
        boolean low = c < 0.1f && (mc.player.tickCount / 8) % 2 == 0;
        g.drawString(font, Component.translatable("item.ls_jetpacks." + j.tier.itemName()), x + 20, y, 0xFFFFFFFF, true);
        GuiDraw.hbar(g, x + 20, y + 11, 68, 4, c, low ? 0xFFFF3030 : j.tier.fuel ? 0xFFE07A2A : 0xFF3CC8F0);
        boolean eng = JetpackItem.engineOn(s);
        g.drawString(font, Component.translatable(eng ? "hud.ls_jetpacks.engine_on" : "hud.ls_jetpacks.engine_off"), x, y + 20, eng ? 0xFF7CFC7C : 0xFFFF7070, true);
        if (j.canHover(s)) {
            boolean hov = JetpackItem.hoverOn(s);
            g.drawString(font, Component.translatable(hov ? "hud.ls_jetpacks.hover_on" : "hud.ls_jetpacks.hover_off"), x + 46, y + 20, hov ? 0xFF7CD8FC : 0xFF909090, true);
        }
        double vy = mc.player.getDeltaMovement().y * 20;
        g.drawString(font, String.format("Y %d  %+.1f m/s", mc.player.getBlockY(), vy), x, y + 31, 0xFFE0E0E0, true);
    }

    // ------------------------------------------------------------------ worn model

    private static void layers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer pr) {
                @SuppressWarnings("unchecked")
                RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent = (RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) (Object) pr;
                pr.addLayer(new WornLayer(parent));
            }
        }
    }

    static class WornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        WornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limb, float limbAmount,
                           float pt, float age, float headYaw, float headPitch) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(stack.getItem() instanceof JetpackItem j) || player.isInvisible()) return;
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(-0.5, -0.75, -0.375);
            RenderUtil.model(ps, buf, new ResourceLocation("ls_jetpacks", "block/worn_" + j.tier.id), light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
    }
}
