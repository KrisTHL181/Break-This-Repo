package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class LongRunningTask implements Runnable {
   protected static final int NUMBER_OF_RETRIES = 25;
   private static final Logger LOGGER = LogUtils.getLogger();
   private boolean aborted = false;

   protected static void pause(long p_167656_) {
      try {
         Thread.sleep(p_167656_ * 1000L);
      } catch (InterruptedException interruptedexception) {
         Thread.currentThread().interrupt();
         LOGGER.error("", interruptedexception);
      }
   }

   public static void setScreen(Screen p_90406_) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.execute(() -> minecraft.setScreen(p_90406_));
   }

   protected void error(Component p_90408_) {
      this.abortTask();
      Minecraft minecraft = Minecraft.getInstance();
   }

   protected void error(Exception p_299436_) {
      if (p_299436_ instanceof RealmsServiceException realmsserviceexception) {
         this.error(realmsserviceexception.realmsError.errorMessage());
      } else {
         this.error(Component.literal(p_299436_.getMessage()));
      }
   }

   protected void error(RealmsServiceException p_298264_) {
      this.error(p_298264_.realmsError.errorMessage());
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
