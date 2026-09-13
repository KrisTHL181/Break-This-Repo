package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ShulkerBulletModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ShulkerBulletRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerBulletRenderer extends EntityRenderer<ShulkerBullet, ShulkerBulletRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/shulker/spark.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE_LOCATION);
   private final ShulkerBulletModel model;

   public ShulkerBulletRenderer(EntityRendererProvider.Context p_174368_) {
      super(p_174368_);
      this.model = new ShulkerBulletModel(p_174368_.bakeLayer(ModelLayers.SHULKER_BULLET));
   }

   protected int getBlockLightLevel(ShulkerBullet p_115869_, BlockPos p_115870_) {
      return 15;
   }

   public void submit(ShulkerBulletRenderState p_427965_, PoseStack p_423475_, SubmitNodeCollector p_429069_, CameraRenderState p_426591_) {
      p_423475_.pushPose();
      float f = p_427965_.ageInTicks;
      p_423475_.translate(0.0F, 0.15F, 0.0F);
      p_423475_.mulPose(Axis.YP.rotationDegrees(Mth.sin(f * 0.1F) * 180.0F));
      p_423475_.mulPose(Axis.XP.rotationDegrees(Mth.cos(f * 0.1F) * 180.0F));
      p_423475_.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f * 0.15F) * 360.0F));
      p_423475_.scale(-0.5F, -0.5F, 0.5F);
      p_429069_.submitModel(
         this.model,
         p_427965_,
         p_423475_,
         this.model.renderType(TEXTURE_LOCATION),
         p_427965_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_427965_.outlineColor,
         null
      );
      p_423475_.scale(1.5F, 1.5F, 1.5F);
      p_429069_.order(1)
         .submitModel(
            this.model, p_427965_, p_423475_, RENDER_TYPE, p_427965_.lightCoords, OverlayTexture.NO_OVERLAY, 654311423, null, p_427965_.outlineColor, null
         );
      p_423475_.popPose();
      super.submit(p_427965_, p_423475_, p_429069_, p_426591_);
   }

   public ShulkerBulletRenderState createRenderState() {
      return new ShulkerBulletRenderState();
   }

   public void extractRenderState(ShulkerBullet p_369782_, ShulkerBulletRenderState p_364377_, float p_361828_) {
      super.extractRenderState(p_369782_, p_364377_, p_361828_);
      p_364377_.yRot = p_369782_.getYRot(p_361828_);
      p_364377_.xRot = p_369782_.getXRot(p_361828_);
   }
}
