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

   public StatsComponent(final MinecraftServer server) {
      this.server = server;
      this.setPreferredSize(new Dimension(456, 246));
      this.setMinimumSize(new Dimension(456, 246));
      this.setMaximumSize(new Dimension(456, 246));
      this.timer = new Timer(500, event -> this.tick());
      this.timer.start();
      this.setBackground(Color.BLACK);
   }

   private void tick() {
      long usedRam = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      this.msgs[0] = "Memory use: "
         + usedRam / 1024L / 1024L
         + " mb ("
         + Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory()
         + "% free)";
      this.msgs[1] = "Avg tick: " + DECIMAL_FORMAT.format((double)this.server.getAverageTickTimeNanos() / TimeUtil.NANOSECONDS_PER_MILLISECOND) + " ms";
      this.values[this.vp++ & 0xFF] = (int)(usedRam * 100L / Runtime.getRuntime().maxMemory());
      this.repaint();
   }

   @Override
   public void paint(final Graphics g) {
      g.setColor(new Color(16777215));
      g.fillRect(0, 0, 456, 246);

      for (int x = 0; x < 256; x++) {
         int v = this.values[x + this.vp & 0xFF];
         g.setColor(new Color(v + 28 << 16));
         g.fillRect(x, 100 - v, 1, v);
      }

      g.setColor(Color.BLACK);

      for (int i = 0; i < this.msgs.length; i++) {
         String msg = this.msgs[i];
         if (msg != null) {
            g.drawString(msg, 32, 116 + i * 16);
         }
      }
   }

   public void close() {
      this.timer.stop();
   }
}
