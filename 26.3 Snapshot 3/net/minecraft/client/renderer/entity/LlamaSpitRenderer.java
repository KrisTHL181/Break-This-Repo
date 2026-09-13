package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.llama.LlamaSpitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LlamaSpitRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.LlamaSpit;

public class LlamaSpitRenderer extends EntityRenderer<LlamaSpit, LlamaSpitRenderState> {
   private static final Identifier LLAMA_SPIT_LOCATION = Identifier.withDefaultNamespace("textures/entity/llama/llama_spit.png");
   private final LlamaSpitModel model;

   public LlamaSpitRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.model = new LlamaSpitModel(context.bakeLayer(ModelLayers.LLAMA_SPIT));
   }

   public void submit(
      final LlamaSpitRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.15F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
      submitNodeCollector.submitModel(this.model, state, poseStack, LLAMA_SPIT_LOCATION, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   public LlamaSpitRenderState createRenderState() {
      return new LlamaSpitRenderState();
   }

   public void extractRenderState(final LlamaSpit entity, final LlamaSpitRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.xRot = entity.getXRot(partialTicks);
      state.yRot = entity.getYRot(partialTicks);
   }
}
