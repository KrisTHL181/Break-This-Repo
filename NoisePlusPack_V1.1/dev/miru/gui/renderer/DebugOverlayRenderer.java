package dev.miru.gui.renderer;

import dev.miru.helper.HardwareInfoCache;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DebugOverlayRenderer {
   private GuiGraphics guiGraphics;
   private Minecraft minecraft;

   public DebugOverlayRenderer(GuiGraphics guiGraphics, Minecraft minecraft) {
      this.guiGraphics = guiGraphics;
      this.minecraft = minecraft;
   }

   public void renderMousePositionTenLine(int mouseX, int mouseY, int color, int lineWidth) {
      this.guiGraphics.fill(mouseX - lineWidth / 2, 0, mouseX + lineWidth / 2, this.minecraft.screen.height, color);
      this.guiGraphics.fill(0, mouseY - lineWidth / 2, this.minecraft.screen.width, mouseY + lineWidth / 2, color);
      this.guiGraphics.drawString(this.minecraft.font, "(%s,%s)".formatted(mouseX, mouseY), mouseX + lineWidth, mouseY + lineWidth, -1);
   }

   public void renderHardWareInfo(int startX, int startY, int lineDistance, int xDelta, int textColor, int subItemColor, HardwareInfoCache hardwareInfoCache) {
      long freeMem = hardwareInfoCache.getSnapshot().freePhysicalMemoryBytes;
      long totalMem = hardwareInfoCache.getSnapshot().totalPhysicalMemoryBytes;
      double cpuLoaded = hardwareInfoCache.getSnapshot().systemCpuLoad;
      List<HardwareInfoCache.GpuInfo> gpuName = hardwareInfoCache.getSnapshot().gpuInfo;
      Map<String, HardwareInfoCache.DiskInfo> diskInfo = hardwareInfoCache.getSnapshot().diskInfo;
      this.renderMemoryInfo(this.minecraft.font, startX, startY, "Memory : ", "%s / %s / %s (Free/Used/Total,Byte)", freeMem, totalMem, textColor, subItemColor);
      int var17;
      int var21;
      this.renderCpuLoadedInfo(
         this.minecraft.font, var17 = startX + xDelta, var21 = startY + lineDistance, "CPU Loaded : ", "%s", cpuLoaded, textColor, subItemColor
      );
      this.renderFps(
         this.minecraft.font, startX = var17 + xDelta, startY = var21 + lineDistance, "FPS : ", "%s", this.minecraft.getFps(), textColor, subItemColor
      );
      int var19;
      int var23;
      int curY = this.renderGpuInfo(
         this.minecraft.font,
         var19 = startX + xDelta,
         var23 = startY + lineDistance,
         xDelta,
         lineDistance,
         "GPU(%s)".formatted(gpuName.size()),
         "| %s",
         gpuName,
         textColor,
         subItemColor
      )[1];
      curY = this.renderDiskInfo(
         this.minecraft.font,
         startX = var19 + xDelta,
         curY,
         xDelta,
         lineDistance,
         "Disk(%s)".formatted(diskInfo.size()),
         "%s | %s / %s / %s (Free/Used/Total)",
         diskInfo,
         textColor,
         subItemColor
      )[1];
   }

   public void renderMemoryInfo(Font font, int x, int y, String tipText, String memoryValueText, long free, long total, int textColor, int valueColor) {
      this.guiGraphics
         .drawString(
            font,
            Component.literal(tipText)
               .withColor(textColor)
               .append(Component.literal(memoryValueText.formatted(free, total - free, total)).withColor(valueColor)),
            x,
            y,
            -1
         );
   }

   public void renderCpuLoadedInfo(Font font, int x, int y, String tipText, String valueText, double loaded, int textColor, int valueColor) {
      this.guiGraphics
         .drawString(
            font, Component.literal(tipText).withColor(textColor).append(Component.literal(valueText.formatted(loaded)).withColor(valueColor)), x, y, -1
         );
   }

   public void renderFps(Font font, int x, int y, String tipText, String valueText, int fps, int textColor, int valueColor) {
      this.guiGraphics
         .drawString(font, Component.literal(tipText).withColor(textColor).append(Component.literal(valueText.formatted(fps)).withColor(valueColor)), x, y, -1);
   }

   public int[] renderGpuInfo(
      Font font,
      int startX,
      int startY,
      int xDelta,
      int lineDistance,
      String tipText,
      String lineText,
      List<HardwareInfoCache.GpuInfo> gpuName,
      int textColor,
      int lineColor
   ) {
      this.guiGraphics.drawString(font, tipText, startX, startY, textColor);

      for (HardwareInfoCache.GpuInfo i : gpuName) {
         this.guiGraphics.drawString(font, lineText.formatted(i.name()), startX += xDelta, startY += lineDistance, lineColor);
      }

      return new int[]{startX, startY + lineDistance};
   }

   public int[] renderDiskInfo(
      Font font,
      int startX,
      int startY,
      int xDelta,
      int lineDistance,
      String tipText,
      String lineText,
      Map<String, HardwareInfoCache.DiskInfo> diskInfos,
      int textColor,
      int lineColor
   ) {
      this.guiGraphics.drawString(font, tipText, startX, startY, textColor);

      for (String key : diskInfos.keySet()) {
         HardwareInfoCache.DiskInfo info = diskInfos.get(key);
         this.guiGraphics
            .drawString(
               font,
               lineText.formatted(key, info.freeBytes(), info.totalBytes() - info.freeBytes(), info.totalBytes()),
               startX += xDelta,
               startY += lineDistance,
               lineColor
            );
      }

      return new int[]{startX, startY + lineDistance};
   }
}
