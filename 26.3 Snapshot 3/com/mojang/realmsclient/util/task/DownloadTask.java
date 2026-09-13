package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class DownloadTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.download.preparing");
   private final long realmId;
   private final int slot;
   private final Screen lastScreen;
   private final String downloadName;

   public DownloadTask(final long realmId, final int slot, final String downloadName, final Screen lastScreen) {
      this.realmId = realmId;
      this.slot = slot;
      this.lastScreen = lastScreen;
      this.downloadName = downloadName;
   }

   @Override
   public void run() {
      RealmsClient client = RealmsClient.getOrCreate();
      int i = 0;

      while (i < 25) {
         try {
            if (this.aborted()) {
               return;
            }

            WorldDownload worldDownload = client.requestDownloadInfo(this.realmId, this.slot);
            pause(1L);
            if (this.aborted()) {
               return;
            }

            setScreen(new RealmsDownloadLatestWorldScreen(this.lastScreen, worldDownload, this.downloadName, result -> {}));
            return;
         } catch (RetryCallException e) {
            if (this.aborted()) {
               return;
            }

            pause(e.delaySeconds);
            i++;
         } catch (RealmsServiceException e) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't download world data", e);
            setScreen(new RealmsGenericErrorScreen(e, this.lastScreen));
            return;
         } catch (Exception e) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't download world data", e);
            this.error(e);
            return;
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
