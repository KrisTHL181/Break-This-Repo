package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmCreationTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.create.world.wait");
   private final String name;
   private final String motd;
   private final long realmId;

   public RealmCreationTask(long p_329245_, String p_335972_, String p_329587_) {
      this.realmId = p_329245_;
      this.name = p_335972_;
      this.motd = p_329587_;
   }

   @Override
   public void run() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         realmsclient.initializeRealm(this.realmId, this.name, this.motd);
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't create world", realmsserviceexception);
         this.error(realmsserviceexception);
      } catch (Exception exception) {
         LOGGER.error("Could not create world", exception);
         this.error(exception);
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
