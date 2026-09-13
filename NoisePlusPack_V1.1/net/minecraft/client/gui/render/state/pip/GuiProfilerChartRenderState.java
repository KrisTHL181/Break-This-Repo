package net.minecraft.client.gui.render.state.pip;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.profiling.ResultField;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiProfilerChartRenderState(
   List<ResultField> chartData, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiProfilerChartRenderState(
      List<ResultField> p_407763_, int p_408191_, int p_406890_, int p_407809_, int p_407639_, @Nullable ScreenRectangle p_410254_
   ) {
      this(
         p_407763_,
         p_408191_,
         p_406890_,
         p_407809_,
         p_407639_,
         p_410254_,
         PictureInPictureRenderState.getBounds(p_408191_, p_406890_, p_407809_, p_407639_, p_410254_)
      );
   }

   @Override
   public float scale() {
      return 1.0F;
   }
}
