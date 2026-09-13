package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class RestoreTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.backup.restoring");
   private final Backup backup;
   private final long realmId;
   private final RealmsConfigureWorldScreen lastScreen;

   public RestoreTask(final Backup backup, final long realmId, final RealmsConfigureWorldScreen lastScreen) {
      this.backup = backup;
      this.realmId = realmId;
      this.lastScreen = lastScreen;
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

            client.restoreWorld(this.realmId, this.backup.backupId);
            pause(1L);
            if (this.aborted()) {
               return;
            }

            setScreen(this.lastScreen);
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

            LOGGER.error("Couldn't restore backup", e);
            setScreen(new RealmsGenericErrorScreen(e, this.lastScreen));
            return;
         } catch (Exception e) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't restore backup", e);
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
