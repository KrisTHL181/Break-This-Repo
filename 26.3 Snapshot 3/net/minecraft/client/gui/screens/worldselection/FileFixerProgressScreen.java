package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class FileFixerProgressScreen extends Screen {
   private static final int PROGRESS_BAR_WIDTH = 200;
   private static final int PROGRESS_BAR_HEIGHT = 2;
   private static final int LINE_SPACING = 3;
   private static final int SECTION_SPACING = 30;
   private static final Component SCANNING = Component.translatable("upgradeWorld.info.scanning");
   private final UpgradeProgress upgradeProgress;
   private Button cancelButton;

   public FileFixerProgressScreen(final UpgradeProgress upgradeProgress) {
      super(Component.translatable("upgradeWorld.title"));
      this.upgradeProgress = upgradeProgress;
   }

   @Override
   protected void init() {
      super.init();
      this.cancelButton = Button.builder(CommonComponents.GUI_CANCEL, button -> {
         this.upgradeProgress.setCanceled();
         button.active = false;
      }).bounds((this.width - 200) / 2, this.height / 2 + 100, 200, 20).build();
      this.addRenderableWidget(this.cancelButton);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int xCenter = this.width / 2;
      int yCenter = this.height / 2;
      int textTop = yCenter - 50;
      this.extractTitle(graphics, xCenter, textTop);
      int totalFiles = this.upgradeProgress.getTotalFileFixStats().totalOperations();
      if (totalFiles > 0) {
         this.extractProgress(graphics, xCenter, textTop);
      } else {
         this.extractScanning(graphics, xCenter, textTop);
      }
   }

   private void extractTitle(final GuiGraphicsExtractor graphics, final int xCenter, final int yTop) {
      graphics.centeredText(this.font, this.title, xCenter, yTop, -1);
   }

   private void extractProgress(final GuiGraphicsExtractor graphics, final int xCenter, final int textTop) {
      UpgradeProgress.FileFixStats typeFileStats = this.upgradeProgress.getTypeFileFixStats();
      UpgradeProgress.FileFixStats totalFileStats = this.upgradeProgress.getTotalFileFixStats();
      UpgradeProgress.FileFixStats runningFileFixerStats = this.upgradeProgress.getRunningFileFixerStats();
      int y = textTop + 9 + 3;
      this.extractProgressBar(graphics, xCenter, y, runningFileFixerStats.getProgress());
      y += 7;
      this.extractFileStats(graphics, xCenter, y, totalFileStats.finishedOperations(), totalFileStats.totalOperations());
      y += 9 * 2 + 6;
      this.extractFileFixerCount(graphics, xCenter, y, runningFileFixerStats.finishedOperations(), runningFileFixerStats.totalOperations());
      y += 9 + 30 - 5;
      this.extractTypeText(graphics, xCenter, y);
      y += 9 + 3;
      this.extractProgressBar(graphics, xCenter, y, typeFileStats.getProgress());
      y += 7;
      this.extractTypeProgress(graphics, xCenter, y, typeFileStats.getProgress());
   }

   private void extractProgressBar(final GuiGraphicsExtractor graphics, final int xCenter, final int y, final float progress) {
      int barLeft = xCenter - 100;
      int barRight = barLeft + 200;
      int barBottom = y + 2;
      graphics.fill(barLeft, y, barRight, barBottom, -16777216);
      graphics.fill(barLeft, y, barLeft + Math.round(progress * 200.0F), barBottom, -16711936);
   }

   private void extractTypeText(final GuiGraphicsExtractor graphics, final int xCenter, final int y) {
      UpgradeProgress.Type upgradeProgressType = this.upgradeProgress.getType();
      if (upgradeProgressType != null) {
         graphics.centeredText(this.font, upgradeProgressType.label(), xCenter, y, -6250336);
      }
   }

   private void extractTypeProgress(final GuiGraphicsExtractor graphics, final int xCenter, final int y, final float progress) {
      Component percentageText = Component.translatable("upgradeWorld.progress.percentage", Mth.floor(progress * 100.0F));
      graphics.centeredText(this.font, percentageText, xCenter, y, -6250336);
   }

   private void extractFileStats(final GuiGraphicsExtractor graphics, final int xCenter, final int yStart, final int converted, final int total) {
      int lineHeight = 9 + 3;
      graphics.centeredText(this.font, Component.translatable("upgradeWorld.info.converted", converted), xCenter, yStart, -6250336);
      graphics.centeredText(this.font, Component.translatable("upgradeWorld.info.total", total), xCenter, yStart + lineHeight, -6250336);
   }

   private void extractScanning(final GuiGraphicsExtractor graphics, final int xCenter, final int textTop) {
      graphics.centeredText(this.font, SCANNING, xCenter, textTop + 9 + 3, -6250336);
   }

   private void extractFileFixerCount(final GuiGraphicsExtractor graphics, final int xCenter, final int y, final int current, final int total) {
      Component percentageText = Component.translatable("upgradeWorld.info.file_fix_stage", current, total);
      graphics.centeredText(this.font, percentageText, xCenter, y, -6250336);
   }
}
