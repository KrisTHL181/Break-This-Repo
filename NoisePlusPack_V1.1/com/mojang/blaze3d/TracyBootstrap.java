package com.mojang.blaze3d;

import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogListeners;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.event.Level;

@OnlyIn(Dist.CLIENT)
public class TracyBootstrap {
   private static boolean setup;

   public static void setup() {
      if (!setup) {
         TracyClient.load();
         if (TracyClient.isAvailable()) {
            LogListeners.addListener("Tracy", (p_361055_, p_365433_) -> TracyClient.message(p_361055_, messageColor(p_365433_)));
            setup = true;
         }
      }
   }

   private static int messageColor(Level p_363373_) {
      return switch (p_363373_) {
         case DEBUG -> 11184810;
         case WARN -> 16777130;
         case ERROR -> 16755370;
         default -> 16777215;
      };
   }
}
