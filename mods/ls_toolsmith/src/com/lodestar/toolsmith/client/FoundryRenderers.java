package com.lodestar.toolsmith.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.toolsmith.foundry.CastingBE;
import com.lodestar.toolsmith.foundry.FaucetBE;
import com.lodestar.toolsmith.foundry.FoundryControllerBE;
import com.lodestar.toolsmith.foundry.FoundryTankBE;
import com.lodestar.core.block.LSEntityBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;

/** Renderers for the foundry pool, tanks, faucet streams and casting tables and basins. */
public final class FoundryRenderers {
    private FoundryRenderers() {}

    /** Draws the molten pool filling the foundry's interior, layered by fluid. */
    public static class Pool implements BlockEntityRenderer<FoundryControllerBE> {
        public Pool(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(FoundryControllerBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (!be.formed || be.fluids.isEmpty() || be.capacity <= 0) return;
            float ox = be.min.getX() - be.getBlockPos().getX(), oy = be.min.getY() - be.getBlockPos().getY(), oz = be.min.getZ() - be.getBlockPos().getZ();
            float w = be.max.getX() - be.min.getX() + 1, d = be.max.getZ() - be.min.getZ() + 1, h = be.max.getY() - be.min.getY() + 1;
            float y = 0;
            for (FluidStack f : be.fluids) {
                float lh = h * f.getAmount() / be.capacity;
                if (lh <= 0.001f) continue;
                RenderUtil.fluidBox(ps, buf, f, ox + 0.01f, oy + y, oz + 0.01f, ox + w - 0.01f, oy + y + lh, oz + d - 0.01f, LightTexture.FULL_BRIGHT);
                y += lh;
            }
        }

        @Override
        public boolean shouldRenderOffScreen(FoundryControllerBE be) {
            return true;
        }
    }

    public static class Tank implements BlockEntityRenderer<FoundryTankBE> {
        public Tank(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(FoundryTankBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            FluidStack f = be.tank.getFluid();
            if (f.isEmpty()) return;
            float h = 0.875f * f.getAmount() / be.tank.getCapacity();
            RenderUtil.fluidBox(ps, buf, f, 0.0625f, 0.0625f, 0.0625f, 0.9375f, 0.0625f + h, 0.9375f, light);
        }
    }

    public static class Faucet implements BlockEntityRenderer<FaucetBE> {
        public Faucet(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(FaucetBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.stream.isEmpty()) return;
            Direction f = LSEntityBlock.facingOf(be.getBlockState());
            float cx = 0.5f + f.getOpposite().getStepX() * 0.18f, cz = 0.5f + f.getOpposite().getStepZ() * 0.18f;
            RenderUtil.fluidBox(ps, buf, be.stream, cx - 0.09f, -0.9f, cz - 0.09f, cx + 0.09f, 0.3f, cz + 0.09f, LightTexture.FULL_BRIGHT);
        }
    }

    public static class Casting implements BlockEntityRenderer<CastingBE> {
        public Casting(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(CastingBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            float top = be.basin ? 0.25f : 1.0f;
            if (!be.cast.isEmpty()) RenderUtil.flatItem(ps, buf, be.cast, 0.5, top + 0.01, 0.5, 0.75f, light, be.getLevel());
            if (!be.output.isEmpty()) {
                if (be.basin) {
                    ps.pushPose();
                    ps.translate(0.5, 0.6, 0.5);
                    RenderUtil.item(ps, buf, be.output, 0.7f, light, be.getLevel(), 0);
                    ps.popPose();
                } else {
                    RenderUtil.flatItem(ps, buf, be.output, 0.5, top + 0.02, 0.5, 0.75f, light, be.getLevel());
                }
            }
            if (!be.fluid.isEmpty() && be.required > 0) {
                float frac = Math.min(1f, (float) be.fluid.getAmount() / be.required);
                if (be.basin) {
                    RenderUtil.fluidBox(ps, buf, be.fluid, 0.125f, 0.25f, 0.125f, 0.875f, 0.25f + 0.7f * frac, 0.875f, LightTexture.FULL_BRIGHT);
                } else {
                    RenderUtil.fluidBox(ps, buf, be.fluid, 0.125f, 1.0f, 0.125f, 0.875f, 1.0f + 0.06f * frac, 0.875f, LightTexture.FULL_BRIGHT);
                }
            }
        }
    }
}
