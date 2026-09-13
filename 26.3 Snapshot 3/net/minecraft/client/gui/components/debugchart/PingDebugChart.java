package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.debugchart.SampleStorage;

public class PingDebugChart extends AbstractDebugChart {
   private static final int CHART_TOP_VALUE = 500;

   public PingDebugChart(final Font font, final SampleStorage sampleStorage) {
      super(font, sampleStorage);
   }

   @Override
   protected void extractAdditionalLinesAndLabels(final GuiGraphicsExtractor graphics, final int left, final int width, final int bottom) {
      this.extractStringWithShade(graphics, "500 ms", left + 1, bottom - 60 + 1);
   }

   @Override
   protected String toDisplayString(final double millis) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(millis));
   }

   @Override
   protected int getSampleHeight(final double millis) {
      return (int)Math.round(millis * 60.0 / 500.0);
   }

   @Override
   protected int getSampleColor(final long millis) {
      return this.getSampleColor(millis, 0.0, -16711936, 250.0, -256, 500.0, -65536);
   }
}
