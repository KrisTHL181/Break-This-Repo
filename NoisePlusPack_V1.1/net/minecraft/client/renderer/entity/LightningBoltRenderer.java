package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LightningBoltRenderer extends EntityRenderer<LightningBolt, LightningBoltRenderState> {
   public LightningBoltRenderer(EntityRendererProvider.Context p_174286_) {
      super(p_174286_);
   }

   public void submit(LightningBoltRenderState p_431298_, PoseStack p_429365_, SubmitNodeCollector p_430011_, CameraRenderState p_431399_) {
      float[] afloat = new float[8];
      float[] afloat1 = new float[8];
      float f = 0.0F;
      float f1 = 0.0F;
      RandomSource randomsource = RandomSource.create(p_431298_.seed);

      for (int i = 7; i >= 0; i--) {
         afloat[i] = f;
         afloat1[i] = f1;
         f += randomsource.nextInt(11) - 5;
         f1 += randomsource.nextInt(11) - 5;
      }

      float f2 = f;
      float f3 = f1;
      p_430011_.submitCustomGeometry(p_429365_, RenderTypes.lightning(), (p_425207_, p_426933_) -> {
         Matrix4f matrix4f = p_425207_.pose();

         for (int j = 0; j < 4; j++) {
            RandomSource randomsource1 = RandomSource.create(p_431298_.seed);

            for (int k = 0; k < 3; k++) {
               int l = 7;
               int i1 = 0;
               if (k > 0) {
                  l = 7 - k;
               }

               if (k > 0) {
                  i1 = l - 2;
               }

               float f4 = afloat[l] - f2;
               float f5 = afloat1[l] - f3;

               for (int j1 = l; j1 >= i1; j1--) {
                  float f6 = f4;
                  float f7 = f5;
                  if (k == 0) {
                     f4 += randomsource1.nextInt(11) - 5;
                     f5 += randomsource1.nextInt(11) - 5;
                  } else {
                     f4 += randomsource1.nextInt(31) - 15;
                     f5 += randomsource1.nextInt(31) - 15;
                  }

                  float f8 = 0.5F;
                  float f9 = 0.45F;
                  float f10 = 0.45F;
                  float f11 = 0.5F;
                  float f12 = 0.1F + j * 0.2F;
                  if (k == 0) {
                     f12 *= j1 * 0.1F + 1.0F;
                  }

                  float f13 = 0.1F + j * 0.2F;
                  if (k == 0) {
                     f13 *= (j1 - 1.0F) * 0.1F + 1.0F;
                  }

                  quad(matrix4f, p_426933_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, false, false, true, false);
                  quad(matrix4f, p_426933_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, true, false, true, true);
                  quad(matrix4f, p_426933_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, true, true, false, true);
                  quad(matrix4f, p_426933_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, false, true, false, false);
               }
            }
         }
      });
   }

   private static void quad(
      Matrix4f p_253966_,
      VertexConsumer p_115274_,
      float p_115275_,
      float p_115276_,
      int p_115277_,
      float p_115278_,
      float p_115279_,
      float p_115280_,
      float p_115281_,
      float p_115282_,
      float p_115283_,
      float p_115284_,
      boolean p_115285_,
      boolean p_115286_,
      boolean p_115287_,
      boolean p_115288_
   ) {
      p_115274_.addVertex(p_253966_, p_115275_ + (p_115285_ ? p_115284_ : -p_115284_), p_115277_ * 16, p_115276_ + (p_115286_ ? p_115284_ : -p_115284_))
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
      p_115274_.addVertex(p_253966_, p_115278_ + (p_115285_ ? p_115283_ : -p_115283_), (p_115277_ + 1) * 16, p_115279_ + (p_115286_ ? p_115283_ : -p_115283_))
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
      p_115274_.addVertex(p_253966_, p_115278_ + (p_115287_ ? p_115283_ : -p_115283_), (p_115277_ + 1) * 16, p_115279_ + (p_115288_ ? p_115283_ : -p_115283_))
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
      p_115274_.addVertex(p_253966_, p_115275_ + (p_115287_ ? p_115284_ : -p_115284_), p_115277_ * 16, p_115276_ + (p_115288_ ? p_115284_ : -p_115284_))
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
   }

   public LightningBoltRenderState createRenderState() {
      return new LightningBoltRenderState();
   }

   public void extractRenderState(LightningBolt p_364798_, LightningBoltRenderState p_367959_, float p_369027_) {
      super.extractRenderState(p_364798_, p_367959_, p_369027_);
      p_367959_.seed = p_364798_.seed;
   }

   protected boolean affectedByCulling(LightningBolt p_365522_) {
      return false;
   }
}
