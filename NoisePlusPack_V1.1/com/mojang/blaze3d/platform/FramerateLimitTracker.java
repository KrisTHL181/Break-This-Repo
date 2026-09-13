package com.mojang.blaze3d.platform;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FramerateLimitTracker {
   private static final int OUT_OF_LEVEL_MENU_LIMIT = 60;
   private static final int ICONIFIED_WINDOW_LIMIT = 10;
   private static final int AFK_LIMIT = 30;
   private static final int LONG_AFK_LIMIT = 10;
   private static final long AFK_THRESHOLD_MS = 60000L;
   private static final long LONG_AFK_THRESHOLD_MS = 600000L;
   private final Options options;
   private final Minecraft minecraft;
   private int framerateLimit;
   private long latestInputTime;

   public FramerateLimitTracker(Options p_364723_, Minecraft p_360737_) {
      this.options = p_364723_;
      this.minecraft = p_360737_;
      this.framerateLimit = p_364723_.framerateLimit().get();
   }

   public int getFramerateLimit() {
      return switch (this.getThrottleReason()) {
         case NONE -> this.framerateLimit;
         case WINDOW_ICONIFIED -> 10;
         case LONG_AFK -> 10;
         case SHORT_AFK -> Math.min(this.framerateLimit, 30);
         case OUT_OF_LEVEL_MENU -> 60;
      };
   }

   public FramerateLimitTracker.FramerateThrottleReason getThrottleReason() {
      InactivityFpsLimit inactivityfpslimit = this.options.inactivityFpsLimit().get();
      if (this.minecraft.getWindow().isIconified()) {
         return FramerateLimitTracker.FramerateThrottleReason.WINDOW_ICONIFIED;
      }

      if (inactivityfpslimit == InactivityFpsLimit.AFK) {
         long i = Util.getMillis() - this.latestInputTime;
         if (i > 600000L) {
            return FramerateLimitTracker.FramerateThrottleReason.LONG_AFK;
         }

         if (i > 60000L) {
            return FramerateLimitTracker.FramerateThrottleReason.SHORT_AFK;
         }
      }

      return this.minecraft.level == null && (this.minecraft.screen != null || this.minecraft.getOverlay() != null)
         ? FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU
         : FramerateLimitTracker.FramerateThrottleReason.NONE;
   }

   public boolean isHeavilyThrottled() {
      FramerateLimitTracker.FramerateThrottleReason frameratelimittracker$frameratethrottlereason = this.getThrottleReason();
      return frameratelimittracker$frameratethrottlereason == FramerateLimitTracker.FramerateThrottleReason.WINDOW_ICONIFIED
         || frameratelimittracker$frameratethrottlereason == FramerateLimitTracker.FramerateThrottleReason.LONG_AFK;
   }

   public void setFramerateLimit(int p_364240_) {
      this.framerateLimit = p_364240_;
   }

   public void onInputReceived() {
      this.latestInputTime = Util.getMillis();
   }

   @OnlyIn(Dist.CLIENT)
   public enum FramerateThrottleReason {
      NONE,
      WINDOW_ICONIFIED,
      LONG_AFK,
      SHORT_AFK,
      OUT_OF_LEVEL_MENU;
   }
}
