package com.mojang.realmsclient.exception;

import com.mojang.realmsclient.client.RealmsError;

public class RetryCallException extends RealmsServiceException {
   public static final int DEFAULT_DELAY = 5;
   public final int delaySeconds;

   public RetryCallException(final int delaySeconds, final int statusCode) {
      super(RealmsError.CustomError.retry(statusCode));
      if (delaySeconds >= 0 && delaySeconds <= 120) {
         this.delaySeconds = delaySeconds;
      } else {
         this.delaySeconds = 5;
      }
   }
}
