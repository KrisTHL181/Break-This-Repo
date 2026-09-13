package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ColoredRectangleRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2fc pose,
   int x0,
   int y0,
   int x1,
   int y1,
   int col1,
   int col2,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public ColoredRectangleRenderState(
      RenderPipeline p_407357_,
      TextureSetup p_410017_,
      Matrix3x2fc p_457928_,
      int p_409034_,
      int p_409937_,
      int p_407075_,
      int p_406246_,
      int p_406832_,
      int p_405982_,
      @Nullable ScreenRectangle p_407734_
   ) {
      this(
         p_407357_,
         p_410017_,
         p_457928_,
         p_409034_,
         p_409937_,
         p_407075_,
         p_406246_,
         p_406832_,
         p_405982_,
         p_407734_,
         getBounds(p_409034_, p_409937_, p_407075_, p_406246_, p_457928_, p_407734_)
      );
   }

   @Override
   public void buildVertices(VertexConsumer p_409842_) {
      p_409842_.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.col1());
      p_409842_.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setColor(this.col2());
      p_409842_.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.col2());
      p_409842_.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setColor(this.col1());
   }

   private static @Nullable ScreenRectangle getBounds(
      int p_409775_, int p_408911_, int p_405873_, int p_405895_, Matrix3x2fc p_457839_, @Nullable ScreenRectangle p_409849_
   ) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_409775_, p_408911_, p_405873_ - p_409775_, p_405895_ - p_408911_).transformMaxBounds(p_457839_);
      return p_409849_ != null ? p_409849_.intersection(screenrectangle) : screenrectangle;
   }
}
