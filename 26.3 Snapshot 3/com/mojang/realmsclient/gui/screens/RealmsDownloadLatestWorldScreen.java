package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.FileDownload;
import com.mojang.realmsclient.dto.WorldDownload;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsDownloadLatestWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ReentrantLock DOWNLOAD_LOCK = new ReentrantLock();
   private static final int BAR_WIDTH = 200;
   private static final int BAR_TOP = 80;
   private static final int BAR_BOTTOM = 95;
   private static final int BAR_BORDER = 1;
   private final Screen lastScreen;
   private final WorldDownload worldDownload;
   private final Component downloadTitle;
   private final RateLimiter narrationRateLimiter;
   private Button cancelButton;
   private final String worldName;
   private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
   private volatile @Nullable Component errorMessage;
   private volatile Component status = Component.translatable("mco.download.preparing");
   private volatile @Nullable String progress;
   private volatile boolean cancelled;
   private volatile boolean showDots = true;
   private volatile boolean finished;
   private volatile boolean extracting;
   private @Nullable Long previousWrittenBytes;
   private @Nullable Long previousTimeSnapshot;
   private long bytesPersSecond;
   private int animTick;
   private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
   private int dotIndex;
   private boolean checked;
   private final BooleanConsumer callback;

   public RealmsDownloadLatestWorldScreen(final Screen lastScreen, final WorldDownload worldDownload, final String worldName, final BooleanConsumer callback) {
      super(GameNarrator.NO_TITLE);
      this.callback = callback;
      this.lastScreen = lastScreen;
      this.worldName = worldName;
      this.worldDownload = worldDownload;
      this.downloadStatus = new RealmsDownloadLatestWorldScreen.DownloadStatus();
      this.downloadTitle = Component.translatable("mco.download.title");
      this.narrationRateLimiter = RateLimiter.create(0.1F);
   }

   @Override
   public void init() {
      this.cancelButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build()
      );
      this.checkDownloadSize();
   }

   private void checkDownloadSize() {
      if (!this.finished && !this.checked) {
         this.checked = true;
         if (this.getContentLength(this.worldDownload.downloadLink()) >= 5368709120L) {
            Component popupMessage = Component.translatable("mco.download.confirmation.oversized", Unit.humanReadable(5368709120L));
            this.minecraft.gui.setScreen(RealmsPopups.warningAcknowledgePopupScreen(this, popupMessage, var1x -> {
               this.minecraft.gui.setScreen(this);
               this.downloadSave();
            }));
         } else {
            this.downloadSave();
         }
      }
   }

   private long getContentLength(final String downloadLink) {
      return FileDownload.contentLength(downloadLink).orElse(0L);
   }

   @Override
   public void tick() {
      super.tick();
      this.animTick++;
      if (this.status != null && this.narrationRateLimiter.tryAcquire(1)) {
         Component message = this.createProgressNarrationMessage();
         this.minecraft.getNarrator().saySystemNow(message);
      }
   }

   private Component createProgressNarrationMessage() {
      List<Component> elements = Lists.newArrayList();
      elements.add(this.downloadTitle);
      elements.add(this.status);
      if (this.progress != null) {
         elements.add(Component.translatable("mco.download.percent", this.progress));
         elements.add(Component.translatable("mco.download.speed.narration", Unit.humanReadable(this.bytesPersSecond)));
      }

      if (this.errorMessage != null) {
         elements.add(this.errorMessage);
      }

      return CommonComponents.joinLines(elements);
   }

   @Override
   public void onClose() {
      this.cancelled = true;
      if (this.finished && this.callback != null && this.errorMessage == null) {
         this.callback.accept(true);
      }

      this.minecraft.gui.setScreen(this.lastScreen);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
      super.extractRenderState(graphics, xm, ym, a);
      graphics.centeredText(this.font, this.downloadTitle, this.width / 2, 20, -1);
      graphics.centeredText(this.font, this.status, this.width / 2, 50, -1);
      if (this.showDots) {
         this.extractDots(graphics);
      }

      if (this.downloadStatus.bytesWritten != 0L && !this.cancelled) {
         this.extractProgressBar(graphics);
         this.extractDownloadSpeed(graphics);
      }

      if (this.errorMessage != null) {
         graphics.centeredText(this.font, this.errorMessage, this.width / 2, 110, -65536);
      }
   }

   private void extractDots(final GuiGraphicsExtractor graphics) {
      int statusWidth = this.font.width(this.status);
      if (this.animTick != 0 && this.animTick % 10 == 0) {
         this.dotIndex++;
      }

      graphics.text(this.font, DOTS[this.dotIndex % DOTS.length], this.width / 2 + statusWidth / 2 + 5, 50, -1);
   }

   private void extractProgressBar(final GuiGraphicsExtractor graphics) {
      double percentage = Math.min((double)this.downloadStatus.bytesWritten / this.downloadStatus.totalBytes, 1.0);
      this.progress = String.format(Locale.ROOT, "%.1f", percentage * 100.0);
      int left = (this.width - 200) / 2;
      int right = left + (int)Math.round(200.0 * percentage);
      graphics.fill(left - 1, 79, right + 1, 96, -1);
      graphics.fill(left, 80, right, 95, -8355712);
      graphics.centeredText(this.font, Component.translatable("mco.download.percent", this.progress), this.width / 2, 84, -1);
   }

   private void extractDownloadSpeed(final GuiGraphicsExtractor graphics) {
      if (this.animTick % 20 == 0) {
         if (this.previousWrittenBytes != null) {
            long timeElapsed = Util.getMillis() - this.previousTimeSnapshot;
            if (timeElapsed == 0L) {
               timeElapsed = 1L;
            }

            this.bytesPersSecond = 1000L * (this.downloadStatus.bytesWritten - this.previousWrittenBytes) / timeElapsed;
            this.extractDownloadSpeed0(graphics, this.bytesPersSecond);
         }

         this.previousWrittenBytes = this.downloadStatus.bytesWritten;
         this.previousTimeSnapshot = Util.getMillis();
      } else {
         this.extractDownloadSpeed0(graphics, this.bytesPersSecond);
      }
   }

   private void extractDownloadSpeed0(final GuiGraphicsExtractor graphics, final long bytesPerSecond) {
      if (bytesPerSecond > 0L) {
         int progressLength = this.font.width(this.progress);
         graphics.text(
            this.font, Component.translatable("mco.download.speed", Unit.humanReadable(bytesPerSecond)), this.width / 2 + progressLength / 2 + 15, 84, -1
         );
      }
   }

   private void downloadSave() {
      new Thread(() -> {
         try {
            try {
               if (!DOWNLOAD_LOCK.tryLock(1L, TimeUnit.SECONDS)) {
                  this.status = Component.translatable("mco.download.failed");
                  return;
               }

               if (this.cancelled) {
                  this.downloadCancelled();
                  return;
               }

               this.status = Component.translatable("mco.download.downloading", this.worldName);
               FileDownload fileDownload = new FileDownload();
               fileDownload.download(this.worldDownload, this.worldName, this.downloadStatus, this.minecraft.getLevelSource());

               while (!fileDownload.isFinished()) {
                  if (fileDownload.isError()) {
                     fileDownload.cancel();
                     this.errorMessage = Component.translatable("mco.download.failed");
                     this.cancelButton.setMessage(CommonComponents.GUI_DONE);
                     return;
                  }

                  if (fileDownload.isExtracting()) {
                     if (!this.extracting) {
                        this.status = Component.translatable("mco.download.extracting");
                     }

                     this.extracting = true;
                  }

                  if (this.cancelled) {
                     fileDownload.cancel();
                     this.downloadCancelled();
                     return;
                  }

                  try {
                     Thread.sleep(500L);
                  } catch (InterruptedException ignored) {
                     LOGGER.error("Failed to check Realms backup download status");
                  }
               }

               this.finished = true;
               this.status = Component.translatable("mco.download.done");
               this.cancelButton.setMessage(CommonComponents.GUI_DONE);
               return;
            } catch (InterruptedException ignored) {
               LOGGER.error("Could not acquire upload lock");
            } catch (Exception e) {
               this.errorMessage = Component.translatable("mco.download.failed");
               LOGGER.info("Exception while downloading world", e);
            }
         } finally {
            if (!DOWNLOAD_LOCK.isHeldByCurrentThread()) {
               return;
            }

            DOWNLOAD_LOCK.unlock();
            this.showDots = false;
            this.finished = true;
         }
      }, "Realms world download monitor").start();
   }

   private void downloadCancelled() {
      this.status = Component.translatable("mco.download.cancelled");
   }

   public static class DownloadStatus {
      public volatile long bytesWritten;
      public volatile long totalBytes;
   }
}
