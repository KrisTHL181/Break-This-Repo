package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ResultField;

public class GuiProfilerChartRenderer extends PictureInPictureRenderer<GuiProfilerChartRenderState> {
   @Override
   public Class<GuiProfilerChartRenderState> getRenderStateClass() {
      return GuiProfilerChartRenderState.class;
   }

   protected void renderToTexture(final GuiProfilerChartRenderState chartState, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector) {
      double totalPercentage = 0.0;
      poseStack.translate(0.0F, -5.0F, 0.0F);

      for (ResultField result : chartState.chartData()) {
         double slicePercentage = result.percentage;
         double currentPercentage = totalPercentage;
         totalPercentage += slicePercentage;
         int steps = Mth.floor(slicePercentage / 4.0) + 1;
         int color = ARGB.opaque(result.getColor());
         int shadeColor = ARGB.multiply(color, -8355712);
         submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.debugTriangleFan(), (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(color);

            for (int j = steps; j >= 0; j--) {
               float dir = (float)((currentPercentage + slicePercentage * j / steps) * (float) (Math.PI * 2) / 100.0);
               float xx = Mth.sin(dir) * 105.0F;
               float yy = Mth.cos(dir) * 105.0F * 0.5F;
               buffer.addVertex(pose, xx, yy, 0.0F).setColor(color);
            }
         });
         submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            for (int j = steps; j > 0; j--) {
               float dir0 = (float)((currentPercentage + slicePercentage * j / steps) * (float) (Math.PI * 2) / 100.0);
               float x0 = Mth.sin(dir0) * 105.0F;
               float y0 = Mth.cos(dir0) * 105.0F * 0.5F;
               float dir1 = (float)((currentPercentage + slicePercentage * (j - 1) / steps) * (float) (Math.PI * 2) / 100.0);
               float x1 = Mth.sin(dir1) * 105.0F;
               float y1 = Mth.cos(dir1) * 105.0F * 0.5F;
               if (!((y0 + y1) / 2.0F < 0.0F)) {
                  buffer.addVertex(pose, x0, y0, 0.0F).setColor(shadeColor);
                  buffer.addVertex(pose, x0, y0 + 10.0F, 0.0F).setColor(shadeColor);
                  buffer.addVertex(pose, x1, y1 + 10.0F, 0.0F).setColor(shadeColor);
                  buffer.addVertex(pose, x1, y1, 0.0F).setColor(shadeColor);
               }
            }
         });
      }
   }

   @Override
   protected float getTranslateY(final int height, final int guiScale) {
      return height / 2.0F;
   }

   @Override
   protected String getTextureLabel() {
      return "profiler chart";
   }
}
