package net.minecraft.client.gui.components.debugchart;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ProfilerPieChart {
   public static final int RADIUS = 105;
   public static final int PIE_CHART_THICKNESS = 10;
   private static final int MARGIN = 5;
   private final Font font;
   private @Nullable ProfileResults profilerPieChartResults;
   private String profilerTreePath = "root";
   private int bottomOffset = 0;

   public ProfilerPieChart(Font p_366678_) {
      this.font = p_366678_;
   }

   public void setPieChartResults(@Nullable ProfileResults p_369896_) {
      this.profilerPieChartResults = p_369896_;
   }

   public void setBottomOffset(int p_366528_) {
      this.bottomOffset = p_366528_;
   }

   public void render(GuiGraphics p_365682_) {
      if (this.profilerPieChartResults != null) {
         List<ResultField> list = this.profilerPieChartResults.getTimes(this.profilerTreePath);
         ResultField resultfield = list.removeFirst();
         int i = p_365682_.guiWidth() - 105 - 10;
         int j = i - 105;
         int k = i + 105;
         int l = list.size() * 9;
         int i1 = p_365682_.guiHeight() - this.bottomOffset - 5;
         int j1 = i1 - l;
         int k1 = 62;
         int l1 = j1 - 62 - 5;
         p_365682_.fill(j - 5, l1 - 62 - 5, k + 5, i1 + 5, -1873784752);
         p_365682_.submitProfilerChartRenderState(list, j, l1 - 62 + 10, k, l1 + 62);
         DecimalFormat decimalformat = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
         String s = ProfileResults.demanglePath(resultfield.name);
         String s1 = "";
         if (!"unspecified".equals(s)) {
            s1 = s1 + "[0] ";
         }

         if (s.isEmpty()) {
            s1 = s1 + "ROOT ";
         } else {
            s1 = s1 + s + " ";
         }

         int i2 = -1;
         int j2 = l1 - 62;
         p_365682_.drawString(this.font, s1, j, j2, -1);
         s1 = decimalformat.format(resultfield.globalPercentage) + "%";
         p_365682_.drawString(this.font, s1, k - this.font.width(s1), j2, -1);

         for (int k2 = 0; k2 < list.size(); k2++) {
            ResultField resultfield1 = list.get(k2);
            StringBuilder stringbuilder = new StringBuilder();
            if ("unspecified".equals(resultfield1.name)) {
               stringbuilder.append("[?] ");
            } else {
               stringbuilder.append("[").append(k2 + 1).append("] ");
            }

            String s2 = stringbuilder.append(resultfield1.name).toString();
            int l2 = j1 + k2 * 9;
            p_365682_.drawString(this.font, s2, j, l2, resultfield1.getColor());
            s2 = decimalformat.format(resultfield1.percentage) + "%";
            p_365682_.drawString(this.font, s2, k - 50 - this.font.width(s2), l2, resultfield1.getColor());
            s2 = decimalformat.format(resultfield1.globalPercentage) + "%";
            p_365682_.drawString(this.font, s2, k - this.font.width(s2), l2, resultfield1.getColor());
         }
      }
   }

   public void profilerPieChartKeyPress(int p_361685_) {
      if (this.profilerPieChartResults != null) {
         List<ResultField> list = this.profilerPieChartResults.getTimes(this.profilerTreePath);
         if (!list.isEmpty()) {
            ResultField resultfield = list.remove(0);
            if (p_361685_ == 0) {
               if (!resultfield.name.isEmpty()) {
                  int i = this.profilerTreePath.lastIndexOf(30);
                  if (i >= 0) {
                     this.profilerTreePath = this.profilerTreePath.substring(0, i);
                  }
               }
            } else {
               p_361685_--;
               if (p_361685_ < list.size() && !"unspecified".equals(list.get(p_361685_).name)) {
                  if (!this.profilerTreePath.isEmpty()) {
                     this.profilerTreePath = this.profilerTreePath + "\u001e";
                  }

                  this.profilerTreePath = this.profilerTreePath + list.get(p_361685_).name;
               }
            }
         }
      }
   }
}
