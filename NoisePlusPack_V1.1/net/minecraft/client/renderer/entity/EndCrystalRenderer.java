package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderer extends EntityRenderer<EndCrystal, EndCrystalRenderState> {
   private static final Identifier END_CRYSTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(END_CRYSTAL_LOCATION);
   private final EndCrystalModel model;

   public EndCrystalRenderer(EntityRendererProvider.Context p_173970_) {
      super(p_173970_);
      this.shadowRadius = 0.5F;
      this.model = new EndCrystalModel(p_173970_.bakeLayer(ModelLayers.END_CRYSTAL));
   }

   public void submit(EndCrystalRenderState p_430915_, PoseStack p_423998_, SubmitNodeCollector p_429866_, CameraRenderState p_426234_) {
      p_423998_.pushPose();
      p_423998_.scale(2.0F, 2.0F, 2.0F);
      p_423998_.translate(0.0F, -0.5F, 0.0F);
      p_429866_.submitModel(this.model, p_430915_, p_423998_, RENDER_TYPE, p_430915_.lightCoords, OverlayTexture.NO_OVERLAY, p_430915_.outlineColor, null);
      p_423998_.popPose();
      Vec3 vec3 = p_430915_.beamOffset;
      if (vec3 != null) {
         float f = getY(p_430915_.ageInTicks);
         float f1 = (float)vec3.x;
         float f2 = (float)vec3.y;
         float f3 = (float)vec3.z;
         p_423998_.translate(vec3);
         EnderDragonRenderer.submitCrystalBeams(-f1, -f2 + f, -f3, p_430915_.ageInTicks, p_423998_, p_429866_, p_430915_.lightCoords);
      }

      super.submit(p_430915_, p_423998_, p_429866_, p_426234_);
   }

   public static float getY(float p_114160_) {
      float f = Mth.sin(p_114160_ * 0.2F) / 2.0F + 0.5F;
      f = (f * f + f) * 0.4F;
      return f - 1.4F;
   }

   public EndCrystalRenderState createRenderState() {
      return new EndCrystalRenderState();
   }

   public void extractRenderState(EndCrystal p_362048_, EndCrystalRenderState p_362246_, float p_367199_) {
      super.extractRenderState(p_362048_, p_362246_, p_367199_);
      p_362246_.ageInTicks = p_362048_.time + p_367199_;
      p_362246_.showsBottom = p_362048_.showsBottom();
      BlockPos blockpos = p_362048_.getBeamTarget();
      if (blockpos != null) {
         p_362246_.beamOffset = Vec3.atCenterOf(blockpos).subtract(p_362048_.getPosition(p_367199_));
      } else {
         p_362246_.beamOffset = null;
      }
   }

   public boolean shouldRender(EndCrystal p_114169_, Frustum p_114170_, double p_114171_, double p_114172_, double p_114173_) {
      return super.shouldRender(p_114169_, p_114170_, p_114171_, p_114172_, p_114173_) || p_114169_.getBeamTarget() != null;
   }
}
