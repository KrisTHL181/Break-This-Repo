package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class RealmCreationTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.create.world.wait");
   private final String name;
   private final String motd;
   private final long realmId;

   public RealmCreationTask(final long realmId, final String name, final String motd) {
      this.realmId = realmId;
      this.name = name;
      this.motd = motd;
   }

   @Override
   public void run() {
      RealmsClient client = RealmsClient.getOrCreate();

      try {
         client.initializeRealm(this.realmId, this.name, this.motd);
      } catch (RealmsServiceException e) {
         LOGGER.error("Couldn't create world", e);
         this.error(e);
      } catch (Exception e) {
         LOGGER.error("Could not create world", e);
         this.error(e);
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
