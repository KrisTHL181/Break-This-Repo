package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.WindChargeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WindChargeRenderer extends EntityRenderer<AbstractWindCharge, EntityRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/wind_charge.png");
   private final WindChargeModel model;

   public WindChargeRenderer(EntityRendererProvider.Context p_311606_) {
      super(p_311606_);
      this.model = new WindChargeModel(p_311606_.bakeLayer(ModelLayers.WIND_CHARGE));
   }

   @Override
   public void submit(EntityRenderState p_427159_, PoseStack p_422608_, SubmitNodeCollector p_430959_, CameraRenderState p_429339_) {
      p_430959_.submitModel(
         this.model,
         p_427159_,
         p_422608_,
         RenderTypes.breezeWind(TEXTURE_LOCATION, this.xOffset(p_427159_.ageInTicks) % 1.0F, 0.0F),
         p_427159_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_427159_.outlineColor,
         null
      );
      super.submit(p_427159_, p_422608_, p_430959_, p_429339_);
   }

   protected float xOffset(float p_311672_) {
      return p_311672_ * 0.03F;
   }

   @Override
   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }
}
