package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FpsDebugChart extends AbstractDebugChart {
   private static final int CHART_TOP_FPS = 30;
   private static final double CHART_TOP_VALUE = 33.333333333333336;

   public FpsDebugChart(Font p_299321_, SampleStorage p_329532_) {
      super(p_299321_, p_329532_);
   }

   @Override
   protected void renderAdditionalLinesAndLabels(GuiGraphics p_298449_, int p_300333_, int p_297224_, int p_301297_) {
      this.drawStringWithShade(p_298449_, "30 FPS", p_300333_ + 1, p_301297_ - 60 + 1);
      this.drawStringWithShade(p_298449_, "60 FPS", p_300333_ + 1, p_301297_ - 30 + 1);
      p_298449_.hLine(p_300333_, p_300333_ + p_297224_ - 1, p_301297_ - 30, -1);
      int i = Minecraft.getInstance().options.framerateLimit().get();
      if (i > 0 && i <= 250) {
         p_298449_.hLine(p_300333_, p_300333_ + p_297224_ - 1, p_301297_ - this.getSampleHeight(1.0E9 / i) - 1, -16711681);
      }
   }

   @Override
   protected String toDisplayString(double p_299977_) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(p_299977_)));
   }

   @Override
   protected int getSampleHeight(double p_301167_) {
      return (int)Math.round(toMilliseconds(p_301167_) * 60.0 / 33.333333333333336);
   }

   @Override
   protected int getSampleColor(long p_299478_) {
      return this.getSampleColor(toMilliseconds(p_299478_), 0.0, -16711936, 28.0, -256, 56.0, -65536);
   }

   private static double toMilliseconds(double p_301228_) {
      return p_301228_ / 1000000.0;
   }
}
