package com.mojang.text2speech;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Narrator {
   Logger LOGGER = LoggerFactory.getLogger(Narrator.class);
   Narrator EMPTY = new Narrator() {
      @Override
      public void say(String msg, boolean interrupt, float volume) {
      }

      @Override
      public void clear() {
      }

      @Override
      public boolean active() {
         return false;
      }

      @Override
      public void destroy() {
      }
   };

   void say(String var1, boolean var2, float var3);

   void clear();

   default boolean active() {
      return true;
   }

   void destroy();

   static Narrator getNarrator() {
      try {
         return switch (OperatingSystem.get()) {
            case LINUX -> new NarratorLinux();
            case WINDOWS -> new NarratorWindows();
            case MAC_OS -> new NarratorMac();
            default -> throw new Narrator.InitializeException("Unsupported platform " + System.getProperty("os.name"));
         };
      } catch (Narrator.FatalException e) {
         throw e;
      } catch (Throwable e) {
         LOGGER.error("Error while loading the narrator", e);
         return EMPTY;
      }
   }

   class FatalException extends RuntimeException {
      public FatalException(String message) {
         super(message);
      }
   }

   class InitializeException extends Exception {
      public InitializeException(String message, Throwable cause) {
         super(message, cause);
      }

      public InitializeException(String message) {
         super(message);
      }
   }
}
