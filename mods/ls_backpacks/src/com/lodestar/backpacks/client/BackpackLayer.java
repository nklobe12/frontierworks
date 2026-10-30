package com.lodestar.backpacks.client;

import com.lodestar.backpacks.BackpackItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

/** Draws a worn backpack on the player's back, dyed. */
public class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BackpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limb, float limbAmount,
                       float pt, float age, float headYaw, float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof BackpackItem bi) || player.isInvisible()) return;
        ps.pushPose();
        getParentModel().body.translateAndRotate(ps);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(-0.5, -0.6875, -0.375);
        int c = bi.getColor(stack);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(new ResourceLocation("ls_backpacks", "block/worn_" + bi.tier.id()));
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()), null, model,
                ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
        ps.popPose();
    }

}
