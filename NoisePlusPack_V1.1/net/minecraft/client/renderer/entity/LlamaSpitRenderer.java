package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.llama.LlamaSpitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LlamaSpitRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaSpitRenderer extends EntityRenderer<LlamaSpit, LlamaSpitRenderState> {
   private static final Identifier LLAMA_SPIT_LOCATION = Identifier.withDefaultNamespace("textures/entity/llama/spit.png");
   private final LlamaSpitModel model;

   public LlamaSpitRenderer(EntityRendererProvider.Context p_174296_) {
      super(p_174296_);
      this.model = new LlamaSpitModel(p_174296_.bakeLayer(ModelLayers.LLAMA_SPIT));
   }

   public void submit(LlamaSpitRenderState p_425573_, PoseStack p_423224_, SubmitNodeCollector p_430561_, CameraRenderState p_430639_) {
      p_423224_.pushPose();
      p_423224_.translate(0.0F, 0.15F, 0.0F);
      p_423224_.mulPose(Axis.YP.rotationDegrees(p_425573_.yRot - 90.0F));
      p_423224_.mulPose(Axis.ZP.rotationDegrees(p_425573_.xRot));
      p_430561_.submitModel(
         this.model,
         p_425573_,
         p_423224_,
         this.model.renderType(LLAMA_SPIT_LOCATION),
         p_425573_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_425573_.outlineColor,
         null
      );
      p_423224_.popPose();
      super.submit(p_425573_, p_423224_, p_430561_, p_430639_);
   }

   public LlamaSpitRenderState createRenderState() {
      return new LlamaSpitRenderState();
   }

   public void extractRenderState(LlamaSpit p_362342_, LlamaSpitRenderState p_368891_, float p_369375_) {
      super.extractRenderState(p_362342_, p_368891_, p_369375_);
      p_368891_.xRot = p_362342_.getXRot(p_369375_);
      p_368891_.yRot = p_362342_.getYRot(p_369375_);
   }
}
