package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public abstract class LongRunningTask implements Runnable {
   protected static final int NUMBER_OF_RETRIES = 25;
   private static final Logger LOGGER = LogUtils.getLogger();
   private boolean aborted = false;

   protected static void pause(final long seconds) {
      try {
         Thread.sleep(seconds * 1000L);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         LOGGER.error("", e);
      }
   }

   public static void setScreen(final Screen screen) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.execute(() -> minecraft.gui.setScreen(screen));
   }

   protected void error(final Component errorMessage) {
      this.abortTask();
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.execute(() -> minecraft.gui.setScreen(new RealmsGenericErrorScreen(errorMessage, new RealmsMainScreen(new TitleScreen()))));
   }

   protected void error(final Exception ex) {
      if (ex instanceof RealmsServiceException rsx) {
         this.error(rsx.realmsError.errorMessage());
      } else {
         this.error(Component.literal(ex.getMessage()));
      }
   }

   protected void error(final RealmsServiceException ex) {
      this.error(ex.realmsError.errorMessage());
   }

   public abstract Component getTitle();

   public boolean aborted() {
      return this.aborted;
   }

   public void tick() {
   }

   public void init() {
   }

   public void abortTask() {
      this.aborted = true;
   }
}
