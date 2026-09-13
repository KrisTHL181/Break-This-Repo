package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TiledBlitRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2f pose,
   int tileWidth,
   int tileHeight,
   int x0,
   int y0,
   int x1,
   int y1,
   float u0,
   float u1,
   float v0,
   float v1,
   int color,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public TiledBlitRenderState(
      RenderPipeline p_431579_,
      TextureSetup p_431534_,
      Matrix3x2f p_422453_,
      int p_424413_,
      int p_431639_,
      int p_429303_,
      int p_430401_,
      int p_429701_,
      int p_423187_,
      float p_425755_,
      float p_426826_,
      float p_428623_,
      float p_429673_,
      int p_428105_,
      @Nullable ScreenRectangle p_426318_
   ) {
      this(
         p_431579_,
         p_431534_,
         p_422453_,
         p_424413_,
         p_431639_,
         p_429303_,
         p_430401_,
         p_429701_,
         p_423187_,
         p_425755_,
         p_426826_,
         p_428623_,
         p_429673_,
         p_428105_,
         p_426318_,
         getBounds(p_429303_, p_430401_, p_429701_, p_423187_, p_422453_, p_426318_)
      );
   }

   @Override
   public void buildVertices(VertexConsumer p_424090_) {
      int i = this.x1() - this.x0();
      int j = this.y1() - this.y0();

      for (int k = 0; k < i; k += this.tileWidth()) {
         int i1 = i - k;
         int l;
         float f;
         if (this.tileWidth() <= i1) {
            l = this.tileWidth();
            f = this.u1();
         } else {
            l = i1;
            f = Mth.lerp((float)i1 / this.tileWidth(), this.u0(), this.u1());
         }

         for (int j1 = 0; j1 < j; j1 += this.tileHeight()) {
            int l1 = j - j1;
            int k1;
            float f1;
            if (this.tileHeight() <= l1) {
               k1 = this.tileHeight();
               f1 = this.v1();
            } else {
               k1 = l1;
               f1 = Mth.lerp((float)l1 / this.tileHeight(), this.v0(), this.v1());
            }

            int i2 = this.x0() + k;
            int j2 = this.x0() + k + l;
            int k2 = this.y0() + j1;
            int l2 = this.y0() + j1 + k1;
            p_424090_.addVertexWith2DPose(this.pose(), i2, k2).setUv(this.u0(), this.v0()).setColor(this.color());
            p_424090_.addVertexWith2DPose(this.pose(), i2, l2).setUv(this.u0(), f1).setColor(this.color());
            p_424090_.addVertexWith2DPose(this.pose(), j2, l2).setUv(f, f1).setColor(this.color());
            p_424090_.addVertexWith2DPose(this.pose(), j2, k2).setUv(f, this.v0()).setColor(this.color());
         }
      }
   }

   private static @Nullable ScreenRectangle getBounds(
      int p_424623_, int p_426569_, int p_427120_, int p_430105_, Matrix3x2f p_424082_, @Nullable ScreenRectangle p_429604_
   ) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_424623_, p_426569_, p_427120_ - p_424623_, p_430105_ - p_426569_).transformMaxBounds(p_424082_);
      return p_429604_ != null ? p_429604_.intersection(screenrectangle) : screenrectangle;
   }
}
