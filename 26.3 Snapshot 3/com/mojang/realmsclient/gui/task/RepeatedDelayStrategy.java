package com.mojang.realmsclient.gui.task;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public interface RepeatedDelayStrategy {
   RepeatedDelayStrategy CONSTANT = new RepeatedDelayStrategy() {
      @Override
      public long delayCyclesAfterSuccess() {
         return 1L;
      }

      @Override
      public long delayCyclesAfterFailure() {
         return 1L;
      }
   };

   long delayCyclesAfterSuccess();

   long delayCyclesAfterFailure();

   static RepeatedDelayStrategy exponentialBackoff(final int maxBackoff) {
      return new RepeatedDelayStrategy() {
         private static final Logger LOGGER = LogUtils.getLogger();
         private int failureCount;

         @Override
         public long delayCyclesAfterSuccess() {
            this.failureCount = 0;
            return 1L;
         }

         @Override
         public long delayCyclesAfterFailure() {
            this.failureCount++;
            long expandedDelay = Math.min(1L << this.failureCount, maxBackoff);
            LOGGER.debug("Skipping for {} extra cycles", expandedDelay);
            return expandedDelay;
         }
      };
   }
}
