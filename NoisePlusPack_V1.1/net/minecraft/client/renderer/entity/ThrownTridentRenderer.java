package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrownTridentRenderer extends EntityRenderer<ThrownTrident, ThrownTridentRenderState> {
   public static final Identifier TRIDENT_LOCATION = Identifier.withDefaultNamespace("textures/entity/trident.png");
   private final TridentModel model;

   public ThrownTridentRenderer(EntityRendererProvider.Context p_174420_) {
      super(p_174420_);
      this.model = new TridentModel(p_174420_.bakeLayer(ModelLayers.TRIDENT));
   }

   public void submit(ThrownTridentRenderState p_429395_, PoseStack p_429602_, SubmitNodeCollector p_423005_, CameraRenderState p_430369_) {
      p_429602_.pushPose();
      p_429602_.mulPose(Axis.YP.rotationDegrees(p_429395_.yRot - 90.0F));
      p_429602_.mulPose(Axis.ZP.rotationDegrees(p_429395_.xRot + 90.0F));
      List<RenderType> list = ItemRenderer.getFoilRenderTypes(this.model.renderType(TRIDENT_LOCATION), false, p_429395_.isFoil);

      for (int i = 0; i < list.size(); i++) {
         p_423005_.order(i)
            .submitModel(
               this.model, Unit.INSTANCE, p_429602_, list.get(i), p_429395_.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, p_429395_.outlineColor, null
            );
      }

      p_429602_.popPose();
      super.submit(p_429395_, p_429602_, p_423005_, p_430369_);
   }

   public ThrownTridentRenderState createRenderState() {
      return new ThrownTridentRenderState();
   }

   public void extractRenderState(ThrownTrident p_461021_, ThrownTridentRenderState p_370121_, float p_366503_) {
      super.extractRenderState(p_461021_, p_370121_, p_366503_);
      p_370121_.yRot = p_461021_.getYRot(p_366503_);
      p_370121_.xRot = p_461021_.getXRot(p_366503_);
      p_370121_.isFoil = p_461021_.isFoil();
   }
}
