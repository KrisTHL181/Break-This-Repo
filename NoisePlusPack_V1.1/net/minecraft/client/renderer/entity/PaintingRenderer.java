package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderer extends EntityRenderer<Painting, PaintingRenderState> {
   private static final Identifier BACK_SPRITE_LOCATION = Identifier.withDefaultNamespace("back");
   private final TextureAtlas paintingsAtlas;

   public PaintingRenderer(EntityRendererProvider.Context p_174332_) {
      super(p_174332_);
      this.paintingsAtlas = p_174332_.getAtlas(AtlasIds.PAINTINGS);
   }

   public void submit(PaintingRenderState p_426919_, PoseStack p_431188_, SubmitNodeCollector p_431178_, CameraRenderState p_423026_) {
      PaintingVariant paintingvariant = p_426919_.variant;
      if (paintingvariant != null) {
         p_431188_.pushPose();
         p_431188_.mulPose(Axis.YP.rotationDegrees(180 - p_426919_.direction.get2DDataValue() * 90));
         TextureAtlasSprite textureatlassprite = this.paintingsAtlas.getSprite(paintingvariant.assetId());
         TextureAtlasSprite textureatlassprite1 = this.paintingsAtlas.getSprite(BACK_SPRITE_LOCATION);
         this.renderPainting(
            p_431188_,
            p_431178_,
            RenderTypes.entitySolidZOffsetForward(textureatlassprite1.atlasLocation()),
            p_426919_.lightCoordsPerBlock,
            paintingvariant.width(),
            paintingvariant.height(),
            textureatlassprite,
            textureatlassprite1
         );
         p_431188_.popPose();
         super.submit(p_426919_, p_431188_, p_431178_, p_423026_);
      }
   }

   public PaintingRenderState createRenderState() {
      return new PaintingRenderState();
   }

   public void extractRenderState(Painting p_459199_, PaintingRenderState p_365628_, float p_360852_) {
      super.extractRenderState(p_459199_, p_365628_, p_360852_);
      Direction direction = p_459199_.getDirection();
      PaintingVariant paintingvariant = p_459199_.getVariant().value();
      p_365628_.direction = direction;
      p_365628_.variant = paintingvariant;
      int i = paintingvariant.width();
      int j = paintingvariant.height();
      if (p_365628_.lightCoordsPerBlock.length != i * j) {
         p_365628_.lightCoordsPerBlock = new int[i * j];
      }

      float f = -i / 2.0F;
      float f1 = -j / 2.0F;
      Level level = p_459199_.level();

      for (int k = 0; k < j; k++) {
         for (int l = 0; l < i; l++) {
            float f2 = l + f + 0.5F;
            float f3 = k + f1 + 0.5F;
            int i1 = p_459199_.getBlockX();
            int j1 = Mth.floor(p_459199_.getY() + f3);
            int k1 = p_459199_.getBlockZ();
            switch (direction) {
               case NORTH:
                  i1 = Mth.floor(p_459199_.getX() + f2);
                  break;
               case WEST:
                  k1 = Mth.floor(p_459199_.getZ() - f2);
                  break;
               case SOUTH:
                  i1 = Mth.floor(p_459199_.getX() - f2);
                  break;
               case EAST:
                  k1 = Mth.floor(p_459199_.getZ() + f2);
            }

            p_365628_.lightCoordsPerBlock[l + k * i] = LevelRenderer.getLightColor(level, new BlockPos(i1, j1, k1));
         }
      }
   }

   private void renderPainting(
      PoseStack p_115559_,
      SubmitNodeCollector p_427389_,
      RenderType p_453537_,
      int[] p_366629_,
      int p_115562_,
      int p_115563_,
      TextureAtlasSprite p_115564_,
      TextureAtlasSprite p_115565_
   ) {
      p_427389_.submitCustomGeometry(p_115559_, p_453537_, (p_430186_, p_431213_) -> {
         float f = -p_115562_ / 2.0F;
         float f1 = -p_115563_ / 2.0F;
         float f2 = 0.03125F;
         float f3 = p_115565_.getU0();
         float f4 = p_115565_.getU1();
         float f5 = p_115565_.getV0();
         float f6 = p_115565_.getV1();
         float f7 = p_115565_.getU0();
         float f8 = p_115565_.getU1();
         float f9 = p_115565_.getV0();
         float f10 = p_115565_.getV(0.0625F);
         float f11 = p_115565_.getU0();
         float f12 = p_115565_.getU(0.0625F);
         float f13 = p_115565_.getV0();
         float f14 = p_115565_.getV1();
         double d0 = 1.0 / p_115562_;
         double d1 = 1.0 / p_115563_;

         for (int i = 0; i < p_115562_; i++) {
            for (int j = 0; j < p_115563_; j++) {
               float f15 = f + (i + 1);
               float f16 = f + i;
               float f17 = f1 + (j + 1);
               float f18 = f1 + j;
               int k = p_366629_[i + j * p_115562_];
               float f19 = p_115564_.getU((float)(d0 * (p_115562_ - i)));
               float f20 = p_115564_.getU((float)(d0 * (p_115562_ - (i + 1))));
               float f21 = p_115564_.getV((float)(d1 * (p_115563_ - j)));
               float f22 = p_115564_.getV((float)(d1 * (p_115563_ - (j + 1))));
               this.vertex(p_430186_, p_431213_, f15, f18, f20, f21, -0.03125F, 0, 0, -1, k);
               this.vertex(p_430186_, p_431213_, f16, f18, f19, f21, -0.03125F, 0, 0, -1, k);
               this.vertex(p_430186_, p_431213_, f16, f17, f19, f22, -0.03125F, 0, 0, -1, k);
               this.vertex(p_430186_, p_431213_, f15, f17, f20, f22, -0.03125F, 0, 0, -1, k);
               this.vertex(p_430186_, p_431213_, f15, f17, f4, f5, 0.03125F, 0, 0, 1, k);
               this.vertex(p_430186_, p_431213_, f16, f17, f3, f5, 0.03125F, 0, 0, 1, k);
               this.vertex(p_430186_, p_431213_, f16, f18, f3, f6, 0.03125F, 0, 0, 1, k);
               this.vertex(p_430186_, p_431213_, f15, f18, f4, f6, 0.03125F, 0, 0, 1, k);
               this.vertex(p_430186_, p_431213_, f15, f17, f7, f9, -0.03125F, 0, 1, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f17, f8, f9, -0.03125F, 0, 1, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f17, f8, f10, 0.03125F, 0, 1, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f17, f7, f10, 0.03125F, 0, 1, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f18, f7, f9, 0.03125F, 0, -1, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f18, f8, f9, 0.03125F, 0, -1, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f18, f8, f10, -0.03125F, 0, -1, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f18, f7, f10, -0.03125F, 0, -1, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f17, f12, f13, 0.03125F, -1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f18, f12, f14, 0.03125F, -1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f18, f11, f14, -0.03125F, -1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f15, f17, f11, f13, -0.03125F, -1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f17, f12, f13, -0.03125F, 1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f18, f12, f14, -0.03125F, 1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f18, f11, f14, 0.03125F, 1, 0, 0, k);
               this.vertex(p_430186_, p_431213_, f16, f17, f11, f13, 0.03125F, 1, 0, 0, k);
            }
         }
      });
   }

   private void vertex(
      PoseStack.Pose p_329838_,
      VertexConsumer p_254114_,
      float p_254164_,
      float p_254459_,
      float p_254183_,
      float p_253615_,
      float p_254448_,
      int p_253660_,
      int p_254342_,
      int p_253757_,
      int p_254101_
   ) {
      p_254114_.addVertex(p_329838_, p_254164_, p_254459_, p_254448_)
         .setColor(-1)
         .setUv(p_254183_, p_253615_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(p_254101_)
         .setNormal(p_329838_, p_253660_, p_254342_, p_253757_);
   }
}
