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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsAvailability {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static @Nullable CompletableFuture<RealmsAvailability.Result> future;

   public static CompletableFuture<RealmsAvailability.Result> get() {
      if (future == null || shouldRefresh(future)) {
         future = check();
      }

      return future;
   }

   private static boolean shouldRefresh(CompletableFuture<RealmsAvailability.Result> p_299038_) {
      RealmsAvailability.Result realmsavailability$result = p_299038_.getNow(null);
      return realmsavailability$result != null && realmsavailability$result.exception() != null;
   }

   private static CompletableFuture<RealmsAvailability.Result> check() {
      if (Minecraft.getInstance().isOfflineDeveloperMode()) {
         return CompletableFuture.completedFuture(new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR));
      } else {
         return SharedConstants.DEBUG_BYPASS_REALMS_VERSION_CHECK
            ? CompletableFuture.completedFuture(new RealmsAvailability.Result(RealmsAvailability.Type.SUCCESS))
            : CompletableFuture.supplyAsync(
               () -> {
                  RealmsClient realmsclient = RealmsClient.getOrCreate();

                  try {
                     if (realmsclient.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                        return new RealmsAvailability.Result(RealmsAvailability.Type.INCOMPATIBLE_CLIENT);
                     } else {
                        return !realmsclient.hasParentalConsent()
                           ? new RealmsAvailability.Result(RealmsAvailability.Type.NEEDS_PARENTAL_CONSENT)
                           : new RealmsAvailability.Result(RealmsAvailability.Type.SUCCESS);
                     }
                  } catch (RealmsServiceException realmsserviceexception) {
                     LOGGER.error("Couldn't connect to realms", realmsserviceexception);
                     return realmsserviceexception.realmsError.errorCode() == 401
                        ? new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR)
                        : new RealmsAvailability.Result(realmsserviceexception);
                  }
               },
               Util.ioPool()
            );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Result(RealmsAvailability.Type type, @Nullable RealmsServiceException exception) {
      public Result(RealmsAvailability.Type p_298726_) {
         this(p_298726_, null);
      }

      public Result(RealmsServiceException p_297806_) {
         this(RealmsAvailability.Type.UNEXPECTED_ERROR, p_297806_);
      }

      public @Nullable Screen createErrorScreen(Screen p_299167_) {
         return switch (this.type) {
            case SUCCESS -> null;
            case INCOMPATIBLE_CLIENT -> new RealmsClientOutdatedScreen(p_299167_);
            case NEEDS_PARENTAL_CONSENT -> new RealmsParentalConsentScreen(p_299167_);
            case AUTHENTICATION_ERROR -> new RealmsGenericErrorScreen(
               Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), p_299167_
            );
            case UNEXPECTED_ERROR -> new RealmsGenericErrorScreen(Objects.requireNonNull(this.exception), p_299167_);
         };
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum Type {
      SUCCESS,
      INCOMPATIBLE_CLIENT,
      NEEDS_PARENTAL_CONSENT,
      AUTHENTICATION_ERROR,
      UNEXPECTED_ERROR;
   }
}
