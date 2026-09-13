package net.minecraft.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeUtil;
import org.jspecify.annotations.Nullable;

public class StatsComponent extends JComponent {
   private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("########0.000", DecimalFormatSymbols.getInstance(Locale.ROOT));
   private final int[] values = new int[256];
   private int vp;
   private final @Nullable String[] msgs = new String[11];
   private final MinecraftServer server;
   private final Timer timer;

   public StatsComponent(MinecraftServer p_139963_) {
      this.server = p_139963_;
      this.setPreferredSize(new Dimension(456, 246));
      this.setMinimumSize(new Dimension(456, 246));
      this.setMaximumSize(new Dimension(456, 246));
      this.timer = new Timer(500, p_139966_ -> this.tick());
      this.timer.start();
      this.setBackground(Color.BLACK);
   }

   private void tick() {
      long i = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      this.msgs[0] = "Memory use: " + i / 1024L / 1024L + " mb (" + Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory() + "% free)";
      this.msgs[1] = "Avg tick: " + DECIMAL_FORMAT.format((double)this.server.getAverageTickTimeNanos() / TimeUtil.NANOSECONDS_PER_MILLISECOND) + " ms";
      this.values[this.vp++ & 0xFF] = (int)(i * 100L / Runtime.getRuntime().maxMemory());
      this.repaint();
   }

   @Override
   public void paint(Graphics p_139973_) {
      p_139973_.setColor(new Color(16777215));
      p_139973_.fillRect(0, 0, 456, 246);

      for (int i = 0; i < 256; i++) {
         int j = this.values[i + this.vp & 0xFF];
         p_139973_.setColor(new Color(j + 28 << 16));
         p_139973_.fillRect(i, 100 - j, 1, j);
      }

      p_139973_.setColor(Color.BLACK);

      for (int k = 0; k < this.msgs.length; k++) {
         String s = this.msgs[k];
         if (s != null) {
            p_139973_.drawString(s, 32, 116 + k * 16);
         }
      }
   }

   public void close() {
      this.timer.stop();
   }
}
