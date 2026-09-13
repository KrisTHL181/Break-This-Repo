package com.mojang.blaze3d.platform;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Util;

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

   public FramerateLimitTracker(final Options options, final Minecraft minecraft) {
      this.options = options;
      this.minecraft = minecraft;
      this.framerateLimit = options.framerateLimit().get();
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
      InactivityFpsLimit inactivityFpsLimit = this.options.inactivityFpsLimit().get();
      if (this.minecraft.getWindow().isIconified()) {
         return FramerateLimitTracker.FramerateThrottleReason.WINDOW_ICONIFIED;
      }

      if (inactivityFpsLimit == InactivityFpsLimit.AFK) {
         long afkTimeMillis = Util.getMillis() - this.latestInputTime;
         if (afkTimeMillis > 600000L) {
            return FramerateLimitTracker.FramerateThrottleReason.LONG_AFK;
         }

         if (afkTimeMillis > 60000L) {
            return FramerateLimitTracker.FramerateThrottleReason.SHORT_AFK;
         }
      }

      return this.minecraft.level != null || this.minecraft.gui.screen() == null && this.minecraft.gui.overlay() == null
         ? FramerateLimitTracker.FramerateThrottleReason.NONE
         : FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU;
   }

   public boolean isHeavilyThrottled() {
      FramerateLimitTracker.FramerateThrottleReason reason = this.getThrottleReason();
      return reason == FramerateLimitTracker.FramerateThrottleReason.WINDOW_ICONIFIED || reason == FramerateLimitTracker.FramerateThrottleReason.LONG_AFK;
   }

   public void setFramerateLimit(final int value) {
      this.framerateLimit = value;
   }

   public void onInputReceived() {
      this.latestInputTime = Util.getMillis();
   }

   public enum FramerateThrottleReason {
      NONE,
      WINDOW_ICONIFIED,
      LONG_AFK,
      SHORT_AFK,
      OUT_OF_LEVEL_MENU;
   }
}
