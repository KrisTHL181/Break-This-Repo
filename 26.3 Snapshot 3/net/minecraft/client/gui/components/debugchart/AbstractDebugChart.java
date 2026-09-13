package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;

public abstract class AbstractDebugChart {
   protected static final int CHART_HEIGHT = 60;
   protected static final int LINE_WIDTH = 1;
   protected final Font font;
   protected final SampleStorage sampleStorage;

   protected AbstractDebugChart(final Font font, final SampleStorage sampleStorage) {
      this.font = font;
      this.sampleStorage = sampleStorage;
   }

   public int getWidth(final int maxWidth) {
      return Math.min(this.sampleStorage.capacity() + 2, maxWidth);
   }

   public int getFullHeight() {
      return 60 + 9;
   }

   public void extractRenderState(final GuiGraphicsExtractor graphics, final int left, final int width) {
      int bottom = graphics.guiHeight();
      graphics.fill(left, bottom - 60, left + width, bottom, -1873784752);
      long avg = 0L;
      long min = 2147483647L;
      long max = -2147483648L;
      int startIndex = Math.max(0, this.sampleStorage.capacity() - (width - 2));
      int sampleCount = this.sampleStorage.size() - startIndex;

      for (int i = 0; i < sampleCount; i++) {
         int currentX = left + i + 1;
         int sampleIndex = startIndex + i;
         long valueForAggregation = this.getValueForAggregation(sampleIndex);
         min = Math.min(min, valueForAggregation);
         max = Math.max(max, valueForAggregation);
         avg += valueForAggregation;
         this.extractSampleBars(graphics, bottom, currentX, sampleIndex);
      }

      graphics.horizontalLine(left, left + width - 1, bottom - 60, -1);
      graphics.horizontalLine(left, left + width - 1, bottom - 1, -1);
      graphics.verticalLine(left, bottom - 60, bottom, -1);
      graphics.verticalLine(left + width - 1, bottom - 60, bottom, -1);
      if (sampleCount > 0) {
         String minText = this.toDisplayString(min) + " min";
         String avgText = this.toDisplayString((double)avg / sampleCount) + " avg";
         String maxText = this.toDisplayString(max) + " max";
         graphics.text(this.font, minText, left + 2, bottom - 60 - 9, -2039584);
         graphics.centeredText(this.font, avgText, left + width / 2, bottom - 60 - 9, -2039584);
         graphics.text(this.font, maxText, left + width - this.font.width(maxText) - 2, bottom - 60 - 9, -2039584);
      }

      this.extractAdditionalLinesAndLabels(graphics, left, width, bottom);
   }

   protected void extractSampleBars(final GuiGraphicsExtractor graphics, final int bottom, final int currentX, final int sampleIndex) {
      this.extractMainSampleBar(graphics, bottom, currentX, sampleIndex);
      this.extractAdditionalSampleBars(graphics, bottom, currentX, sampleIndex);
   }

   protected void extractMainSampleBar(final GuiGraphicsExtractor graphics, final int bottom, final int currentX, final int sampleIndex) {
      long value = this.sampleStorage.get(sampleIndex);
      int sampleHeight = this.getSampleHeight(value);
      int color = this.getSampleColor(value);
      graphics.fill(currentX, bottom - sampleHeight, currentX + 1, bottom, color);
   }

   protected void extractAdditionalSampleBars(final GuiGraphicsExtractor graphics, final int bottom, final int currentX, final int sampleIndex) {
   }

   protected long getValueForAggregation(final int sampleIndex) {
      return this.sampleStorage.get(sampleIndex);
   }

   protected void extractAdditionalLinesAndLabels(final GuiGraphicsExtractor graphics, final int left, final int width, final int bottom) {
   }

   protected void extractStringWithShade(final GuiGraphicsExtractor graphics, final String str, final int x, final int y) {
      graphics.fill(x, y, x + this.font.width(str) + 1, y + 9, -1873784752);
      graphics.text(this.font, str, x + 1, y + 1, -2039584, false);
   }

   protected abstract String toDisplayString(double sample);

   protected abstract int getSampleHeight(double sample);

   protected abstract int getSampleColor(long sample);

   protected int getSampleColor(double sample, final double min, final int minColor, final double mid, final int midColor, final double max, final int maxColor) {
      sample = Mth.clamp(sample, min, max);
      return sample < mid
         ? ARGB.srgbLerp((float)((sample - min) / (mid - min)), minColor, midColor)
         : ARGB.srgbLerp((float)((sample - mid) / (max - mid)), midColor, maxColor);
   }
}
