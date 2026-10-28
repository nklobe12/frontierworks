package com.lodestar.kinetics.client;

import com.lodestar.core.block.LSEntityBlock;
import com.lodestar.core.client.RenderUtil;
import com.lodestar.kinetics.AirBlowerBE;
import com.lodestar.kinetics.KineticBE;
import com.lodestar.kinetics.KineticMachineBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/** Renderers for the shaft-driven machines: the mill's runner stone, the press ram, the vat paddle and contents, the blower. */
public final class MachineRenderers {
    private MachineRenderers() {}

    private static float angle(float prev, float rpm, float pt) {
        return (float) ((prev + rpm * KineticBE.DEG_PER_TICK * pt) % 360.0);
    }

    public static class Machine implements BlockEntityRenderer<KineticMachineBE> {
        public Machine(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(KineticMachineBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            float a = angle(be.clientAnglePrev, be.rpm(), pt);
            switch (be.kind) {
                case MILL -> KRender.rotor(ps, buf, "grist_mill_stone", Direction.Axis.Y, a, light, overlay);
                case PRESS -> press(be, a, ps, buf, light, overlay);
                case VAT -> vat(be, a, ps, buf, light, overlay);
            }
        }

        private static void press(KineticMachineBE be, float a, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ItemStack in = be.shownInput(0);
            boolean working = be.rpm() != 0 && !in.isEmpty();
            float depth = working ? (float) (1 - Math.cos(Math.toRadians(a))) / 2f : 0f;
            depth = depth * depth;
            if (!in.isEmpty()) RenderUtil.flatItem(ps, buf, in, 0.5, 0.26, 0.5, 0.45f, light, be.getLevel());
            ps.pushPose();
            ps.translate(0, 4 / 16f + 0.02f + (5 / 16f) * (1 - depth), 0);
            RenderUtil.model(ps, buf, KRender.model("stamping_press_ram"), light, overlay);
            ps.popPose();
        }

        private static void vat(KineticMachineBE be, float a, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            float y = 2 / 16f;
            FluidTank[] tanks = be.getTanks();
            for (FluidTank t : tanks) {
                FluidStack f = t.getFluid();
                if (f.isEmpty()) continue;
                float h = (13 / 16f) * f.getAmount() / Math.max(1f, 3 * t.getCapacity() / 2f);
                if (h <= 0.002f) continue;
                RenderUtil.fluidBox(ps, buf, f, 2 / 16f + 0.002f, y, 2 / 16f + 0.002f, 14 / 16f - 0.002f, Math.min(15 / 16f, y + h), 14 / 16f - 0.002f, light);
                y = Math.min(15 / 16f, y + h);
            }
            for (int i = 0; i < 4; i++) {
                ItemStack s = be.shownInput(i);
                if (s.isEmpty()) continue;
                double r = Math.toRadians(a * 0.5 + i * 90);
                ps.pushPose();
                ps.translate(0.5 + 0.22 * Math.cos(r), 0.35 + 0.03 * Math.sin(r * 3), 0.5 + 0.22 * Math.sin(r));
                ps.mulPose(Axis.YP.rotationDegrees(-a * 0.5f - i * 90));
                RenderUtil.item(ps, buf, s, 0.28f, light, be.getLevel(), i);
                ps.popPose();
            }
            KRender.rotor(ps, buf, "mixing_vat_paddle", Direction.Axis.Y, a, light, overlay);
        }
    }

    public static class Blower implements BlockEntityRenderer<AirBlowerBE> {
        public Blower(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(AirBlowerBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Direction f = LSEntityBlock.facingOf(be.getBlockState());
            float a = (float) ((be.clientAnglePrev + be.rpm() * KineticBE.DEG_PER_TICK * 2 * pt) % 360.0);
            KRender.facingRotor(ps, buf, "air_blower_rotor", f, a, light, overlay);
        }
    }
}
