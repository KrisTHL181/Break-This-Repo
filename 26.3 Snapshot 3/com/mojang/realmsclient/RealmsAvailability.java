package com.mojang.realmsclient;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsAvailability {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static @Nullable CompletableFuture<RealmsAvailability.Result> future;

   public static CompletableFuture<RealmsAvailability.Result> get() {
      if (future == null || shouldRefresh(future)) {
         future = check();
      }

      return future;
   }

   private static boolean shouldRefresh(final CompletableFuture<RealmsAvailability.Result> future) {
      RealmsAvailability.Result result = future.getNow(null);
      return result != null && result.exception() != null;
   }

   private static CompletableFuture<RealmsAvailability.Result> check() {
      if (Minecraft.getInstance().isOfflineDeveloperMode()) {
         return CompletableFuture.completedFuture(new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR));
      } else {
         return SharedConstants.DEBUG_BYPASS_REALMS_VERSION_CHECK
            ? CompletableFuture.completedFuture(new RealmsAvailability.Result(RealmsAvailability.Type.SUCCESS))
            : CompletableFuture.supplyAsync(
               () -> {
                  RealmsClient client = RealmsClient.getOrCreate();

                  try {
                     if (client.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                        return new RealmsAvailability.Result(RealmsAvailability.Type.INCOMPATIBLE_CLIENT);
                     } else {
                        return !client.hasParentalConsent()
                           ? new RealmsAvailability.Result(RealmsAvailability.Type.NEEDS_PARENTAL_CONSENT)
                           : new RealmsAvailability.Result(RealmsAvailability.Type.SUCCESS);
                     }
                  } catch (RealmsServiceException e) {
                     LOGGER.error("Couldn't connect to realms", e);
                     return e.realmsError.errorCode() == 401
                        ? new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR)
                        : new RealmsAvailability.Result(e);
                  }
               },
               Util.nonCriticalIoPool()
            );
      }
   }

   public record Result(RealmsAvailability.Type type, @Nullable RealmsServiceException exception) {
      public Result(final RealmsAvailability.Type type) {
         this(type, null);
      }

      public Result(final RealmsServiceException exception) {
         this(RealmsAvailability.Type.UNEXPECTED_ERROR, exception);
      }

      public @Nullable Screen createErrorScreen(final Screen lastScreen) {
         return switch (this.type) {
            case SUCCESS -> null;
            case INCOMPATIBLE_CLIENT -> new RealmsClientOutdatedScreen(lastScreen);
            case NEEDS_PARENTAL_CONSENT -> new RealmsParentalConsentScreen(lastScreen);
            case AUTHENTICATION_ERROR -> new RealmsGenericErrorScreen(
               Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), lastScreen
            );
            case UNEXPECTED_ERROR -> new RealmsGenericErrorScreen(Objects.requireNonNull(this.exception), lastScreen);
         };
      }
   }

   public enum Type {
      SUCCESS,
      INCOMPATIBLE_CLIENT,
      NEEDS_PARENTAL_CONSENT,
      AUTHENTICATION_ERROR,
      UNEXPECTED_ERROR;
   }
}
