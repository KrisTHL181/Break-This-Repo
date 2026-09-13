package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.effects.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerFangsRenderer extends EntityRenderer<EvokerFangs, EvokerFangsRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");
   private final EvokerFangsModel model;

   public EvokerFangsRenderer(EntityRendererProvider.Context p_174100_) {
      super(p_174100_);
      this.model = new EvokerFangsModel(p_174100_.bakeLayer(ModelLayers.EVOKER_FANGS));
   }

   public void submit(EvokerFangsRenderState p_423536_, PoseStack p_425309_, SubmitNodeCollector p_425064_, CameraRenderState p_427925_) {
      float f = p_423536_.biteProgress;
      if (f != 0.0F) {
         p_425309_.pushPose();
         p_425309_.mulPose(Axis.YP.rotationDegrees(90.0F - p_423536_.yRot));
         p_425309_.scale(-1.0F, -1.0F, 1.0F);
         p_425309_.translate(0.0F, -1.501F, 0.0F);
         p_425064_.submitModel(
            this.model,
            p_423536_,
            p_425309_,
            this.model.renderType(TEXTURE_LOCATION),
            p_423536_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_423536_.outlineColor,
            null
         );
         p_425309_.popPose();
         super.submit(p_423536_, p_425309_, p_425064_, p_427925_);
      }
   }

   public EvokerFangsRenderState createRenderState() {
      return new EvokerFangsRenderState();
   }

   public void extractRenderState(EvokerFangs p_369816_, EvokerFangsRenderState p_364298_, float p_361549_) {
      super.extractRenderState(p_369816_, p_364298_, p_361549_);
      p_364298_.yRot = p_369816_.getYRot();
      p_364298_.biteProgress = p_369816_.getAnimationProgress(p_361549_);
   }
}
