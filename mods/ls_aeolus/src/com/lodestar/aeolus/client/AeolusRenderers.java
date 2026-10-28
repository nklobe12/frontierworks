package com.lodestar.aeolus.client;

import com.lodestar.aeolus.AnemometerBE;
import com.lodestar.aeolus.RotorItem;
import com.lodestar.aeolus.TidalGeneratorBE;
import com.lodestar.aeolus.WaterWheelBE;
import com.lodestar.aeolus.WindTurbineBE;
import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.client.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Spinning rotors, wheels, propellers and the anemometer, drawn from baked block models. */
public final class AeolusRenderers {
    private AeolusRenderers() {}

    static final ResourceLocation HUB = new ResourceLocation("ls_aeolus", "block/rotor_hub");
    static final ResourceLocation WHEEL_ARM = new ResourceLocation("ls_aeolus", "block/water_wheel_arm");
    static final ResourceLocation TIDAL_BLADE = new ResourceLocation("ls_aeolus", "block/tidal_blade");
    static final ResourceLocation CUPS = new ResourceLocation("ls_aeolus", "block/anemometer_cups");
    static final ResourceLocation VANE = new ResourceLocation("ls_aeolus", "block/anemometer_vane");

    static List<ResourceLocation> models() {
        return List.of(HUB, WHEEL_ARM, TIDAL_BLADE, CUPS, VANE,
                blade("wooden_rotor"), blade("canvas_rotor"), blade("bronze_rotor"), blade("steel_rotor"), blade("duralumin_rotor"));
    }

    static ResourceLocation blade(String rotor) {
        return new ResourceLocation("ls_aeolus", "block/" + rotor + "_blade");
    }

    /** Rotates the pose so a model built facing north faces the given horizontal direction (pose origin at block centre). */
    static void face(PoseStack ps, Direction f) {
        ps.mulPose(Axis.YP.rotationDegrees(180f - f.toYRot()));
    }

    static int frontLight(BlockEntity be, Direction f) {
        return be.getLevel() == null ? 0xF000F0 : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(f));
    }

    public static class Turbine implements BlockEntityRenderer<WindTurbineBE> {
        public Turbine(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(WindTurbineBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (!(be.rotorItem instanceof RotorItem r)) return;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(r);
            if (id == null) return;
            Direction f = LSEntityBlock.facingOf(be.getBlockState());
            int l = frontLight(be, f);
            float ang = Mth.lerp(pt, be.prevAngle, be.angle);
            ResourceLocation blade = new ResourceLocation(id.getNamespace(), "block/" + id.getPath() + "_blade");
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            face(ps, f);
            ps.mulPose(Axis.ZP.rotationDegrees(ang));
            ps.pushPose();
            ps.translate(-0.5, -0.5, -0.5);
            RenderUtil.model(ps, buf, HUB, l, overlay);
            ps.popPose();
            for (int i = 0; i < r.blades; i++) {
                ps.pushPose();
                ps.mulPose(Axis.ZP.rotationDegrees(i * 360f / r.blades));
                ps.mulPose(Axis.YP.rotationDegrees(14f));
                ps.scale(1f, r.radius, 1f);
                ps.translate(-0.5, -0.5, -0.5);
                RenderUtil.model(ps, buf, blade, l, overlay);
                ps.popPose();
            }
            ps.popPose();
        }

        @Override
        public boolean shouldRenderOffScreen(WindTurbineBE be) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return 128;
        }
    }

    public static class Wheel implements BlockEntityRenderer<WaterWheelBE> {
        public Wheel(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(WaterWheelBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Direction f = LSEntityBlock.facingOf(be.getBlockState());
            float ang = Mth.lerp(pt, be.prevAngle, be.angle);
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            face(ps, f);
            ps.mulPose(Axis.ZP.rotationDegrees(ang));
            for (int i = 0; i < 8; i++) {
                ps.pushPose();
                ps.mulPose(Axis.ZP.rotationDegrees(i * 45f));
                ps.translate(-0.5, -0.5, -0.5);
                RenderUtil.model(ps, buf, WHEEL_ARM, light, overlay);
                ps.popPose();
            }
            ps.popPose();
        }

        @Override
        public boolean shouldRenderOffScreen(WaterWheelBE be) {
            return true;
        }
    }

    public static class Tidal implements BlockEntityRenderer<TidalGeneratorBE> {
        public Tidal(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(TidalGeneratorBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Direction f = LSEntityBlock.facingOf(be.getBlockState());
            int l = frontLight(be, f);
            float ang = Mth.lerp(pt, be.prevAngle, be.angle);
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            face(ps, f);
            ps.mulPose(Axis.ZP.rotationDegrees(ang));
            for (int i = 0; i < 3; i++) {
                ps.pushPose();
                ps.mulPose(Axis.ZP.rotationDegrees(i * 120f));
                ps.mulPose(Axis.YP.rotationDegrees(20f));
                ps.scale(1f, 0.5f, 1f);
                ps.translate(-0.5, -0.5, -0.5);
                RenderUtil.model(ps, buf, TIDAL_BLADE, l, overlay);
                ps.popPose();
            }
            ps.popPose();
        }
    }

    public static class Anemometer implements BlockEntityRenderer<AnemometerBE> {
        public Anemometer(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(AnemometerBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            float ang = Mth.lerp(pt, be.prevAngle, be.angle);
            float vane = be.prevVane + (((be.vane - be.prevVane) % 360f + 540f) % 360f - 180f) * pt;
            int l = be.getLevel() == null ? light : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-ang));
            ps.translate(-0.5, -0.5, -0.5);
            RenderUtil.model(ps, buf, CUPS, l, overlay);
            ps.popPose();
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-vane));
            ps.translate(-0.5, -0.5, -0.5);
            RenderUtil.model(ps, buf, VANE, l, overlay);
            ps.popPose();
        }
    }
}
