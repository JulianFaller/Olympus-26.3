package dev.xylonity.olympus.client.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class PoseidonCrownRenderer implements ICurioRenderer {

    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
            final ItemStack stack,
            final SlotContext slotContext,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int packedLight,
            final S renderState,
            final RenderLayerParent<S, M> renderLayerParent,
            final EntityRendererProvider.Context context,
            final float yRotation,
            final float xRotation
    ) {
        final ItemStackRenderState itemRenderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(itemRenderState, stack, ItemDisplayContext.HEAD, slotContext.entity());
        poseStack.pushPose();
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.head.translateAndRotate(poseStack);
        }
        itemRenderState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
    }

}
