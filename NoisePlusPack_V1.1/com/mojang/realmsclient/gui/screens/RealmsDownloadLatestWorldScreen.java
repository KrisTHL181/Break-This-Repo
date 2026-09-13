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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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

   public RealmsDownloadLatestWorldScreen(Screen p_88625_, WorldDownload p_88626_, String p_88627_, BooleanConsumer p_88628_) {
      super(GameNarrator.NO_TITLE);
      this.callback = p_88628_;
      this.lastScreen = p_88625_;
      this.worldName = p_88627_;
      this.worldDownload = p_88626_;
      this.downloadStatus = new RealmsDownloadLatestWorldScreen.DownloadStatus();
      this.downloadTitle = Component.translatable("mco.download.title");
      this.narrationRateLimiter = RateLimiter.create(0.1F);
   }

   @Override
   public void init() {
      this.cancelButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_340720_ -> this.onClose()).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build()
      );
      this.checkDownloadSize();
   }

   private void checkDownloadSize() {
      if (!this.finished && !this.checked) {
         this.checked = true;
         if (this.getContentLength(this.worldDownload.downloadLink()) >= 5368709120L) {
            Component component = Component.translatable("mco.download.confirmation.oversized", Unit.humanReadable(5368709120L));
            this.minecraft.setScreen(RealmsPopups.warningAcknowledgePopupScreen(this, component, p_340719_ -> {
               this.minecraft.setScreen(this);
               this.downloadSave();
            }));
         } else {
            this.downloadSave();
         }
      }
   }

   private long getContentLength(String p_88647_) {
      return FileDownload.contentLength(p_88647_).orElse(0L);
   }

   @Override
   public void tick() {
      super.tick();
      this.animTick++;
      if (this.status != null && this.narrationRateLimiter.tryAcquire(1)) {
         Component component = this.createProgressNarrationMessage();
         this.minecraft.getNarrator().saySystemNow(component);
      }
   }

   private Component createProgressNarrationMessage() {
      List<Component> list = Lists.newArrayList();
      list.add(this.downloadTitle);
      list.add(this.status);
      if (this.progress != null) {
         list.add(Component.translatable("mco.download.percent", this.progress));
         list.add(Component.translatable("mco.download.speed.narration", Unit.humanReadable(this.bytesPersSecond)));
      }

      if (this.errorMessage != null) {
         list.add(this.errorMessage);
      }

      return CommonComponents.joinLines(list);
   }

   @Override
   public void onClose() {
      this.cancelled = true;
      if (this.finished && this.callback != null && this.errorMessage == null) {
         this.callback.accept(true);
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void render(GuiGraphics p_282124_, int p_88635_, int p_88636_, float p_88637_) {
      super.render(p_282124_, p_88635_, p_88636_, p_88637_);
      p_282124_.drawCenteredString(this.font, this.downloadTitle, this.width / 2, 20, -1);
      p_282124_.drawCenteredString(this.font, this.status, this.width / 2, 50, -1);
      if (this.showDots) {
         this.drawDots(p_282124_);
      }

      if (this.downloadStatus.bytesWritten != 0L && !this.cancelled) {
         this.drawProgressBar(p_282124_);
         this.drawDownloadSpeed(p_282124_);
      }

      if (this.errorMessage != null) {
         p_282124_.drawCenteredString(this.font, this.errorMessage, this.width / 2, 110, -65536);
      }
   }

   private void drawDots(GuiGraphics p_281948_) {
      int i = this.font.width(this.status);
      if (this.animTick != 0 && this.animTick % 10 == 0) {
         this.dotIndex++;
      }

      p_281948_.drawString(this.font, DOTS[this.dotIndex % DOTS.length], this.width / 2 + i / 2 + 5, 50, -1);
   }

   private void drawProgressBar(GuiGraphics p_281556_) {
      double d0 = Math.min((double)this.downloadStatus.bytesWritten / this.downloadStatus.totalBytes, 1.0);
      this.progress = String.format(Locale.ROOT, "%.1f", d0 * 100.0);
      int i = (this.width - 200) / 2;
      int j = i + (int)Math.round(200.0 * d0);
      p_281556_.fill(i - 1, 79, j + 1, 96, -1);
      p_281556_.fill(i, 80, j, 95, -8355712);
      p_281556_.drawCenteredString(this.font, Component.translatable("mco.download.percent", this.progress), this.width / 2, 84, -1);
   }

   private void drawDownloadSpeed(GuiGraphics p_282236_) {
      if (this.animTick % 20 == 0) {
         if (this.previousWrittenBytes != null) {
            long i = Util.getMillis() - this.previousTimeSnapshot;
            if (i == 0L) {
               i = 1L;
            }

            this.bytesPersSecond = 1000L * (this.downloadStatus.bytesWritten - this.previousWrittenBytes) / i;
            this.drawDownloadSpeed0(p_282236_, this.bytesPersSecond);
         }

         this.previousWrittenBytes = this.downloadStatus.bytesWritten;
         this.previousTimeSnapshot = Util.getMillis();
      } else {
         this.drawDownloadSpeed0(p_282236_, this.bytesPersSecond);
      }
   }

   private void drawDownloadSpeed0(GuiGraphics p_283338_, long p_281931_) {
      if (p_281931_ > 0L) {
         int i = this.font.width(this.progress);
         p_283338_.drawString(this.font, Component.translatable("mco.download.speed", Unit.humanReadable(p_281931_)), this.width / 2 + i / 2 + 15, 84, -1);
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
               FileDownload filedownload = new FileDownload();
               filedownload.download(this.worldDownload, this.worldName, this.downloadStatus, this.minecraft.getLevelSource());

               while (!filedownload.isFinished()) {
                  if (filedownload.isError()) {
                     filedownload.cancel();
                     this.errorMessage = Component.translatable("mco.download.failed");
                     this.cancelButton.setMessage(CommonComponents.GUI_DONE);
                     return;
                  }

                  if (filedownload.isExtracting()) {
                     if (!this.extracting) {
                        this.status = Component.translatable("mco.download.extracting");
                     }

                     this.extracting = true;
                  }

                  if (this.cancelled) {
                     filedownload.cancel();
                     this.downloadCancelled();
                     return;
                  }

                  try {
                     Thread.sleep(500L);
                  } catch (InterruptedException interruptedexception) {
                     LOGGER.error("Failed to check Realms backup download status");
                  }
               }

               this.finished = true;
               this.status = Component.translatable("mco.download.done");
               this.cancelButton.setMessage(CommonComponents.GUI_DONE);
               return;
            } catch (InterruptedException interruptedexception1) {
               LOGGER.error("Could not acquire upload lock");
            } catch (Exception exception) {
               this.errorMessage = Component.translatable("mco.download.failed");
               LOGGER.info("Exception while downloading world", exception);
            }
         } finally {
            if (!DOWNLOAD_LOCK.isHeldByCurrentThread()) {
               return;
            }

            DOWNLOAD_LOCK.unlock();
            this.showDots = false;
            this.finished = true;
         }
      }).start();
   }

   private void downloadCancelled() {
      this.status = Component.translatable("mco.download.cancelled");
   }

   @OnlyIn(Dist.CLIENT)
   public static class DownloadStatus {
      public volatile long bytesWritten;
      public volatile long totalBytes;
   }
}
