package com.lodestar.kinetics.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.kinetics.AxleBE;
import com.lodestar.kinetics.KineticBE;
import com.lodestar.kinetics.PulleyBE;
import com.lodestar.kinetics.SpurGearBE;
import com.lodestar.kinetics.WindmillHubBE;
import com.lodestar.kinetics.HandWheelBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Block entity renderers for network members: everything that turns is drawn here, at the network's angle. */
public final class NodeRenderers {
    private NodeRenderers() {}

    private static Direction.Axis axisOf(KineticBE be) {
        var s = be.getBlockState();
        return s.hasProperty(BlockStateProperties.AXIS) ? s.getValue(BlockStateProperties.AXIS) : Direction.Axis.Y;
    }

    /** A single rotor model along the block's axis (axles, couplings, gauges, gears, paddle wheels). */
    public static class Rotor<T extends KineticBE> implements BlockEntityRenderer<T> {
        private final String model;

        public Rotor(BlockEntityRendererProvider.Context ctx, String model) {
            this.model = model;
        }

        @Override
        public void render(T be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            float a = be.angle(pt);
            if (be instanceof SpurGearBE g) a += g.toothOffset();
            Direction.Axis ax = axisOf(be);
            KRender.rotor(ps, buf, model, ax, a, light, overlay);
            if (be instanceof AxleBE ab && (ab.mode == AxleBE.Mode.TACHOMETER || ab.mode == AxleBE.Mode.DYNAMOMETER)) needles(ps, buf, ab, ax);
        }

        @Override
        public boolean shouldRenderOffScreen(T be) {
            return model.startsWith("paddle");
        }

        private static void needles(PoseStack ps, MultiBufferSource buf, AxleBE be, Direction.Axis ax) {
            float sweep = -135 + 270 * be.gaugeValue();
            int col = be.mode == AxleBE.Mode.DYNAMOMETER && be.status() == KineticBE.OVERLOADED ? 0xFFE02020 : 0xFF202020;
            for (Direction s : Direction.values()) {
                if (s.getAxis() == ax) continue;
                ps.pushPose();
                ps.translate(0.5, 0.5, 0.5);
                switch (s) {
                    case SOUTH -> {}
                    case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
                    case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
                    case WEST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
                    case UP -> ps.mulPose(Axis.XP.rotationDegrees(-90));
                    case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(90));
                }
                ps.translate(0, -0.0625, 0.3135);
                ps.mulPose(Axis.ZP.rotationDegrees(-sweep));
                RenderUtil.colorBox(ps, buf, -0.012f, -0.01f, 0f, 0.012f, 0.28f, 0.006f, col);
                RenderUtil.colorBox(ps, buf, -0.03f, -0.03f, 0f, 0.03f, 0.03f, 0.008f, 0xFF3A3A3A);
                ps.popPose();
            }
        }
    }

    /** Shaft stubs on every face with a shaft (bevel junction, gear train, motor), each at its face's speed. */
    public static class Stubs<T extends KineticBE> implements BlockEntityRenderer<T> {
        public Stubs(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(T be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            float a = be.angle(pt);
            for (Direction d : Direction.values()) {
                double ff = be.faceFactor(d);
                if (ff == 0) continue;
                KRender.facingRotor(ps, buf, "shaft_stub", d, (float) ((a * ff) % 360.0), light, overlay);
            }
        }
    }

    public static class HandWheel implements BlockEntityRenderer<HandWheelBE> {
        public HandWheel(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(HandWheelBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Direction f = com.lodestar.core.block.LSEntityBlock.facingOf(be.getBlockState());
            KRender.facingRotor(ps, buf, "hand_wheel_rotor", f, be.angle(pt), light, overlay);
        }
    }

    /** Pulley wheel plus, from the lower pulley of a pair, the two runs of the belt. */
    public static class Pulley implements BlockEntityRenderer<PulleyBE> {
        public Pulley(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(PulleyBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Direction.Axis ax = axisOf(be);
            KRender.rotor(ps, buf, "pulley_rotor", ax, be.angle(pt), light, overlay);
            if (!be.drawsBelt()) return;
            BlockPos o = be.partner();
            Vec3 c1 = new Vec3(0.5, 0.5, 0.5);
            Vec3 c2 = c1.add(Vec3.atLowerCornerOf(o.subtract(be.getBlockPos())));
            Vec3 a = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(ax, Direction.AxisDirection.POSITIVE).getNormal());
            Vec3 d = c2.subtract(c1);
            if (d.lengthSqr() < 1e-4) return;
            Vec3 n = a.cross(d).normalize();
            Vec3 w = a.scale(0.17);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 off = n.scale(side * 0.33);
                Vec3 p1 = c1.add(off), p2 = c2.add(off);
                KRender.quad(ps, buf, p1.subtract(w), p1.add(w), p2.add(w), p2.subtract(w), 0xFF5C3B22);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(PulleyBE be) {
            return be.drawsBelt();
        }
    }

    /** The windmill rotor head and its raised sail, turning in the plane in front of the hub. */
    public static class Windmill implements BlockEntityRenderer<WindmillHubBE> {
        public Windmill(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(WindmillHubBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (!be.assembled()) return;
            Direction f = com.lodestar.core.block.LSEntityBlock.facingOf(be.getBlockState());
            Direction across = f.getClockWise();
            float a = be.angle(pt);
            float yrot = switch (f) {
                case EAST -> -90;
                case SOUTH -> 180;
                case WEST -> 90;
                default -> 0;
            };
            ps.pushPose();
            ps.translate(0.5 + f.getStepX(), 0.5, 0.5 + f.getStepZ());
            KRender.spin(ps, f.getAxis(), a);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(yrot));
            ps.translate(-0.5, -0.5, -0.5);
            RenderUtil.model(ps, buf, KRender.model("windmill_rotor"), light, overlay);
            ps.popPose();
            for (int[] c : be.vanes()) {
                if (c[0] == 0 && c[1] == 0) continue;
                ps.pushPose();
                ps.translate(across.getStepX() * c[0], c[1], across.getStepZ() * c[0]);
                ps.scale(0.999f, 0.999f, 0.999f);
                ps.translate(-0.5, -0.5, -0.5);
                RenderUtil.model(ps, buf, KRender.model("windmill_vane"), light, overlay);
                ps.popPose();
            }
            ps.popPose();
        }

        @Override
        public boolean shouldRenderOffScreen(WindmillHubBE be) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return 128;
        }
    }
}
