package com.mojang.realmsclient.util;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
   private static final int MINUTES = 60;
   private static final int HOURS = 3600;
   private static final int DAYS = 86400;

   public static Component convertToAgePresentation(long p_287679_) {
      if (p_287679_ < 0L) {
         return RIGHT_NOW;
      } else {
         long i = p_287679_ / 1000L;
         if (i < 60L) {
            return Component.translatable("mco.time.secondsAgo", i);
         } else if (i < 3600L) {
            long l = i / 60L;
            return Component.translatable("mco.time.minutesAgo", l);
         } else if (i < 86400L) {
            long k = i / 3600L;
            return Component.translatable("mco.time.hoursAgo", k);
         } else {
            long j = i / 86400L;
            return Component.translatable("mco.time.daysAgo", j);
         }
      }
   }

   public static Component convertToAgePresentationFromInstant(Instant p_452143_) {
      return convertToAgePresentation(System.currentTimeMillis() - p_452143_.toEpochMilli());
   }

   public static void renderPlayerFace(GuiGraphics p_281255_, int p_281818_, int p_281791_, int p_282088_, UUID p_298294_) {
      PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = Minecraft.getInstance()
         .playerSkinRenderCache()
         .getOrDefault(ResolvableProfile.createUnresolved(p_298294_));
      PlayerFaceRenderer.draw(p_281255_, playerskinrendercache$renderinfo.playerSkin(), p_281818_, p_281791_, p_282088_);
   }

   public static <T> CompletableFuture<T> supplyAsync(RealmsUtil.RealmsIoFunction<T> p_407261_, @Nullable Consumer<RealmsServiceException> p_409160_) {
      return CompletableFuture.supplyAsync(() -> {
         RealmsClient realmsclient = RealmsClient.getOrCreate();

         try {
            return p_407261_.apply(realmsclient);
         } catch (Throwable throwable) {
            if (throwable instanceof RealmsServiceException realmsserviceexception) {
               if (p_409160_ != null) {
                  p_409160_.accept(realmsserviceexception);
               }
            } else {
               LOGGER.error("Unhandled exception", throwable);
            }

            throw new RuntimeException(throwable);
         }
      }, Util.nonCriticalIoPool());
   }

   public static CompletableFuture<Void> runAsync(RealmsUtil.RealmsIoConsumer p_407814_, @Nullable Consumer<RealmsServiceException> p_407267_) {
      return supplyAsync(p_407814_, p_407267_);
   }

   public static Consumer<RealmsServiceException> openScreenOnFailure(Function<RealmsServiceException, Screen> p_408464_) {
      Minecraft minecraft = Minecraft.getInstance();
      return p_410171_ -> minecraft.execute(() -> minecraft.setScreen(p_408464_.apply(p_410171_)));
   }

   public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(Function<RealmsServiceException, Screen> p_410235_, String p_408788_) {
      return openScreenOnFailure(p_410235_).andThen(p_408019_ -> LOGGER.error(p_408788_, p_408019_));
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface RealmsIoConsumer extends RealmsUtil.RealmsIoFunction<Void> {
      void accept(RealmsClient var1) throws RealmsServiceException;

      default Void apply(RealmsClient p_407565_) throws RealmsServiceException {
         this.accept(p_407565_);
         return null;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface RealmsIoFunction<T> {
      T apply(RealmsClient var1) throws RealmsServiceException;
   }
}
