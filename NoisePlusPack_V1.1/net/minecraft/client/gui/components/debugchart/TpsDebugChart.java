package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TpsDebugChart extends AbstractDebugChart {
   private static final int TICK_METHOD_COLOR = -6745839;
   private static final int TASK_COLOR = -4548257;
   private static final int OTHER_COLOR = -10547572;
   private final Supplier<Float> msptSupplier;

   public TpsDebugChart(Font p_298557_, SampleStorage p_332350_, Supplier<Float> p_309657_) {
      super(p_298557_, p_332350_);
      this.msptSupplier = p_309657_;
   }

   @Override
   protected void renderAdditionalLinesAndLabels(GuiGraphics p_297354_, int p_298051_, int p_298343_, int p_299488_) {
      float f = (float)TimeUtil.MILLISECONDS_PER_SECOND / this.msptSupplier.get();
      this.drawStringWithShade(p_297354_, String.format(Locale.ROOT, "%.1f TPS", f), p_298051_ + 1, p_299488_ - 60 + 1);
   }

   @Override
   protected void drawAdditionalDimensions(GuiGraphics p_330453_, int p_332124_, int p_334033_, int p_330538_) {
      long i = this.sampleStorage.get(p_330538_, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
      int j = this.getSampleHeight(i);
      p_330453_.fill(p_334033_, p_332124_ - j, p_334033_ + 1, p_332124_, -6745839);
      long k = this.sampleStorage.get(p_330538_, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
      int l = this.getSampleHeight(k);
      p_330453_.fill(p_334033_, p_332124_ - j - l, p_334033_ + 1, p_332124_ - j, -4548257);
      long i1 = this.sampleStorage.get(p_330538_) - this.sampleStorage.get(p_330538_, TpsDebugDimensions.IDLE.ordinal()) - i - k;
      int j1 = this.getSampleHeight(i1);
      p_330453_.fill(p_334033_, p_332124_ - j1 - l - j, p_334033_ + 1, p_332124_ - l - j, -10547572);
   }

   @Override
   protected long getValueForAggregation(int p_335820_) {
      return this.sampleStorage.get(p_335820_) - this.sampleStorage.get(p_335820_, TpsDebugDimensions.IDLE.ordinal());
   }

   @Override
   protected String toDisplayString(double p_301254_) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(p_301254_)));
   }

   @Override
   protected int getSampleHeight(double p_299260_) {
      return (int)Math.round(toMilliseconds(p_299260_) * 60.0 / this.msptSupplier.get().floatValue());
   }

   @Override
   protected int getSampleColor(long p_300761_) {
      float f = this.msptSupplier.get();
      return this.getSampleColor(toMilliseconds(p_300761_), f, -16711936, f * 1.125, -256, f * 1.25, -65536);
   }

   private static double toMilliseconds(double p_300655_) {
      return p_300655_ / 1000000.0;
   }
}
