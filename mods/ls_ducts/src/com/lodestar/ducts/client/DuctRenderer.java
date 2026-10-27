package com.lodestar.ducts.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.ducts.Attachment;
import com.lodestar.ducts.DuctBE;
import com.lodestar.ducts.DuctKind;
import com.lodestar.ducts.TravellingItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Draws attachments on duct sides and the items travelling through item ducts. */
public class DuctRenderer implements BlockEntityRenderer<DuctBE> {
    public static final List<String> MODELS = List.of("attachment_servo", "attachment_filter", "attachment_retriever",
            "attachment_fluid_servo", "attachment_fluid_filter");

    public DuctRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(DuctBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        boolean fluid = be.kind == DuctKind.FLUID;
        for (Direction d : Direction.values()) {
            Attachment a = be.attachment(d);
            if (a == null) continue;
            String name = "attachment_" + (fluid ? "fluid_" : "") + a.type.name().toLowerCase(java.util.Locale.ROOT);
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            switch (d) {
                case NORTH -> {}
                case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case EAST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
                case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case UP -> ps.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(-90));
            }
            ps.translate(-0.5, -0.5, -0.5);
            RenderUtil.model(ps, buf, new ResourceLocation("ls_ducts", "block/" + name), light, overlay);
            ps.popPose();
        }
        if (be.kind != DuctKind.ITEM) return;
        int i = 0;
        for (TravellingItem ti : be.items) {
            float p = Math.min(1f, ti.progress + ti.speed * pt);
            Direction dir = ti.dir;
            ps.pushPose();
            ps.translate(0.5 + dir.getStepX() * p, 0.5 + dir.getStepY() * p, 0.5 + dir.getStepZ() * p);
            RenderUtil.item(ps, buf, ti.stack, 0.32f, light, be.getLevel(), i++);
            ps.popPose();
        }
    }
}
