package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class StuckInBodyLayer<M extends PlayerModel, S> extends RenderLayer<AvatarRenderState, M> {
   private final Model<S> model;
   private final S modelState;
   private final Identifier texture;
   private final StuckInBodyLayer.PlacementStyle placementStyle;

   public StuckInBodyLayer(
      LivingEntityRenderer<?, AvatarRenderState, M> p_117564_, Model<S> p_360738_, S p_422391_, Identifier p_459830_, StuckInBodyLayer.PlacementStyle p_363745_
   ) {
      super(p_117564_);
      this.model = p_360738_;
      this.modelState = p_422391_;
      this.texture = p_459830_;
      this.placementStyle = p_363745_;
   }

   protected abstract int numStuck(AvatarRenderState var1);

   private void submitStuckItem(
      PoseStack p_426306_, SubmitNodeCollector p_426792_, int p_429763_, float p_425425_, float p_424731_, float p_429968_, int p_425459_
   ) {
      float f = Mth.sqrt(p_425425_ * p_425425_ + p_429968_ * p_429968_);
      float f1 = (float)(Math.atan2(p_425425_, p_429968_) * 180.0 / (float) Math.PI);
      float f2 = (float)(Math.atan2(p_424731_, f) * 180.0 / (float) Math.PI);
      p_426306_.mulPose(Axis.YP.rotationDegrees(f1 - 90.0F));
      p_426306_.mulPose(Axis.ZP.rotationDegrees(f2));
      p_426792_.submitModel(this.model, this.modelState, p_426306_, this.model.renderType(this.texture), p_429763_, OverlayTexture.NO_OVERLAY, p_425459_, null);
   }

   public void submit(PoseStack p_422390_, SubmitNodeCollector p_425616_, int p_429518_, AvatarRenderState p_423439_, float p_429981_, float p_425230_) {
      int i = this.numStuck(p_423439_);
      if (i > 0) {
         RandomSource randomsource = RandomSource.create(p_423439_.id);

         for (int j = 0; j < i; j++) {
            p_422390_.pushPose();
            ModelPart modelpart = this.getParentModel().getRandomBodyPart(randomsource);
            ModelPart.Cube modelpart$cube = modelpart.getRandomCube(randomsource);
            modelpart.translateAndRotate(p_422390_);
            float f = randomsource.nextFloat();
            float f1 = randomsource.nextFloat();
            float f2 = randomsource.nextFloat();
            if (this.placementStyle == StuckInBodyLayer.PlacementStyle.ON_SURFACE) {
               int k = randomsource.nextInt(3);
               switch (k) {
                  case 0:
                     f = snapToFace(f);
                     break;
                  case 1:
                     f1 = snapToFace(f1);
                     break;
                  default:
                     f2 = snapToFace(f2);
               }
            }

            p_422390_.translate(
               Mth.lerp(f, modelpart$cube.minX, modelpart$cube.maxX) / 16.0F,
               Mth.lerp(f1, modelpart$cube.minY, modelpart$cube.maxY) / 16.0F,
               Mth.lerp(f2, modelpart$cube.minZ, modelpart$cube.maxZ) / 16.0F
            );
            this.submitStuckItem(p_422390_, p_425616_, p_429518_, -(f * 2.0F - 1.0F), -(f1 * 2.0F - 1.0F), -(f2 * 2.0F - 1.0F), p_423439_.outlineColor);
            p_422390_.popPose();
         }
      }
   }

   private static float snapToFace(float p_362675_) {
      return p_362675_ > 0.5F ? 1.0F : 0.5F;
   }

   @OnlyIn(Dist.CLIENT)
   public enum PlacementStyle {
      IN_CUBE,
      ON_SURFACE;
   }
}
