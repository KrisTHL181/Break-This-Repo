package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatRenderer extends EntityRenderer<AbstractBoat, BoatRenderState> {
   public AbstractBoatRenderer(EntityRendererProvider.Context p_366587_) {
      super(p_366587_);
      this.shadowRadius = 0.8F;
   }

   public void submit(BoatRenderState p_429224_, PoseStack p_426753_, SubmitNodeCollector p_429321_, CameraRenderState p_429640_) {
      p_426753_.pushPose();
      p_426753_.translate(0.0F, 0.375F, 0.0F);
      p_426753_.mulPose(Axis.YP.rotationDegrees(180.0F - p_429224_.yRot));
      float f = p_429224_.hurtTime;
      if (f > 0.0F) {
         p_426753_.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * p_429224_.damageTime / 10.0F * p_429224_.hurtDir));
      }

      if (!p_429224_.isUnderWater && !Mth.equal(p_429224_.bubbleAngle, 0.0F)) {
         p_426753_.mulPose(new Quaternionf().setAngleAxis(p_429224_.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
      }

      p_426753_.scale(-1.0F, -1.0F, 1.0F);
      p_426753_.mulPose(Axis.YP.rotationDegrees(90.0F));
      p_429321_.submitModel(
         this.model(), p_429224_, p_426753_, this.renderType(), p_429224_.lightCoords, OverlayTexture.NO_OVERLAY, p_429224_.outlineColor, null
      );
      this.submitTypeAdditions(p_429224_, p_426753_, p_429321_, p_429224_.lightCoords);
      p_426753_.popPose();
      super.submit(p_429224_, p_426753_, p_429321_, p_429640_);
   }

   protected void submitTypeAdditions(BoatRenderState p_361534_, PoseStack p_362579_, SubmitNodeCollector p_427942_, int p_361807_) {
   }

   protected abstract EntityModel<BoatRenderState> model();

   protected abstract RenderType renderType();

   public BoatRenderState createRenderState() {
      return new BoatRenderState();
   }

   public void extractRenderState(AbstractBoat p_454761_, BoatRenderState p_368077_, float p_364654_) {
      super.extractRenderState(p_454761_, p_368077_, p_364654_);
      p_368077_.yRot = p_454761_.getYRot(p_364654_);
      p_368077_.hurtTime = p_454761_.getHurtTime() - p_364654_;
      p_368077_.hurtDir = p_454761_.getHurtDir();
      p_368077_.damageTime = Math.max(p_454761_.getDamage() - p_364654_, 0.0F);
      p_368077_.bubbleAngle = p_454761_.getBubbleAngle(p_364654_);
      p_368077_.isUnderWater = p_454761_.isUnderWater();
      p_368077_.rowingTimeLeft = p_454761_.getRowingTime(0, p_364654_);
      p_368077_.rowingTimeRight = p_454761_.getRowingTime(1, p_364654_);
   }
}
