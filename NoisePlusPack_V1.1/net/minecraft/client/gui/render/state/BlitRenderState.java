package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlitRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2f pose,
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
      RenderPipeline p_410610_,
      TextureSetup p_408028_,
      Matrix3x2f p_408213_,
      int p_410101_,
      int p_408250_,
      int p_409655_,
      int p_410715_,
      float p_409605_,
      float p_407208_,
      float p_407399_,
      float p_409036_,
      int p_407112_,
      @Nullable ScreenRectangle p_408495_
   ) {
      this(
         p_410610_,
         p_408028_,
         p_408213_,
         p_410101_,
         p_408250_,
         p_409655_,
         p_410715_,
         p_409605_,
         p_407208_,
         p_407399_,
         p_409036_,
         p_407112_,
         p_408495_,
         getBounds(p_410101_, p_408250_, p_409655_, p_410715_, p_408213_, p_408495_)
      );
   }

   @Override
   public void buildVertices(VertexConsumer p_407042_) {
      p_407042_.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(this.u0(), this.v0()).setColor(this.color());
      p_407042_.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(this.u0(), this.v1()).setColor(this.color());
      p_407042_.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(this.u1(), this.v1()).setColor(this.color());
      p_407042_.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(this.u1(), this.v0()).setColor(this.color());
   }

   private static @Nullable ScreenRectangle getBounds(
      int p_409231_, int p_406918_, int p_409531_, int p_409172_, Matrix3x2f p_410341_, @Nullable ScreenRectangle p_408074_
   ) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_409231_, p_406918_, p_409531_ - p_409231_, p_409172_ - p_406918_).transformMaxBounds(p_410341_);
      return p_408074_ != null ? p_408074_.intersection(screenrectangle) : screenrectangle;
   }
}
