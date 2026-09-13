package net.minecraft.client.renderer.state.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

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
      final RenderPipeline pipeline,
      final TextureSetup textureSetup,
      final Matrix3x2f pose,
      final int tileWidth,
      final int tileHeight,
      final int x0,
      final int y0,
      final int x1,
      final int y1,
      final float u0,
      final float u1,
      final float v0,
      final float v1,
      final int color,
      final @Nullable ScreenRectangle scissorArea
   ) {
      this(
         pipeline, textureSetup, pose, tileWidth, tileHeight, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea)
      );
   }

   @Override
   public void buildVertices(final VertexConsumer vertexConsumer) {
      int width = this.x1() - this.x0();
      int height = this.y1() - this.y0();

      for (int tileX = 0; tileX < width; tileX += this.tileWidth()) {
         int remainingWidth = width - tileX;
         int tileWidth;
         float u1;
         if (this.tileWidth() <= remainingWidth) {
            tileWidth = this.tileWidth();
            u1 = this.u1();
         } else {
            tileWidth = remainingWidth;
            u1 = Mth.lerp((float)remainingWidth / this.tileWidth(), this.u0(), this.u1());
         }

         for (int tileY = 0; tileY < height; tileY += this.tileHeight()) {
            int remainingHeight = height - tileY;
            int tileHeight;
            float v1;
            if (this.tileHeight() <= remainingHeight) {
               tileHeight = this.tileHeight();
               v1 = this.v1();
            } else {
               tileHeight = remainingHeight;
               v1 = Mth.lerp((float)remainingHeight / this.tileHeight(), this.v0(), this.v1());
            }

            int x0 = this.x0() + tileX;
            int x1 = this.x0() + tileX + tileWidth;
            int y0 = this.y0() + tileY;
            int y1 = this.y0() + tileY + tileHeight;
            vertexConsumer.addVertexWith2DPose(this.pose(), x0, y0).setUv(this.u0(), this.v0()).setColor(this.color());
            vertexConsumer.addVertexWith2DPose(this.pose(), x0, y1).setUv(this.u0(), v1).setColor(this.color());
            vertexConsumer.addVertexWith2DPose(this.pose(), x1, y1).setUv(u1, v1).setColor(this.color());
            vertexConsumer.addVertexWith2DPose(this.pose(), x1, y0).setUv(u1, this.v0()).setColor(this.color());
         }
      }
   }

   private static @Nullable ScreenRectangle getBounds(
      final int x0, final int y0, final int x1, final int y1, final Matrix3x2f pose, final @Nullable ScreenRectangle scissorArea
   ) {
      ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
   }
}
