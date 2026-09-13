package net.minecraft.client.renderer.state.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record BlitRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2fc pose,
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
   public BlitRenderState(
      final RenderPipeline pipeline,
      final TextureSetup textureSetup,
      final Matrix3x2f pose,
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
      this(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Override
   public void buildVertices(final VertexConsumer vertexConsumer) {
      vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(this.u0(), this.v0()).setColor(this.color());
      vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(this.u0(), this.v1()).setColor(this.color());
      vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(this.u1(), this.v1()).setColor(this.color());
      vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(this.u1(), this.v0()).setColor(this.color());
   }

   private static @Nullable ScreenRectangle getBounds(
      final int x0, final int y0, final int x1, final int y1, final Matrix3x2f pose, final @Nullable ScreenRectangle scissorArea
   ) {
      ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
   }
}
