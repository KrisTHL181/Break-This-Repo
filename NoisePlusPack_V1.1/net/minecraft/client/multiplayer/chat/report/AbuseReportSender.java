package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface AbuseReportSender {
   static AbuseReportSender create(ReportEnvironment p_239536_, UserApiService p_239537_) {
      return new AbuseReportSender.Services(p_239536_, p_239537_);
   }

   CompletableFuture<Unit> send(UUID var1, ReportType var2, AbuseReport var3);

   boolean isEnabled();

   default AbuseReportLimits reportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }

   @OnlyIn(Dist.CLIENT)
   class SendException extends ThrowingComponent {
      public SendException(Component p_239646_, Throwable p_239647_) {
         super(p_239646_, p_239647_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
      private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
      private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
      private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

      @Override
      public CompletableFuture<Unit> send(UUID p_239470_, ReportType p_297714_, AbuseReport p_239471_) {
         return CompletableFuture.supplyAsync(
            () -> {
               AbuseReportRequest abusereportrequest = new AbuseReportRequest(
                  1,
                  p_239470_,
                  p_239471_,
                  this.environment.clientInfo(),
                  this.environment.thirdPartyServerInfo(),
                  this.environment.realmInfo(),
                  p_297714_.backendName()
               );

               try {
                  this.userApiService.reportAbuse(abusereportrequest);
                  return Unit.INSTANCE;
               } catch (MinecraftClientHttpException minecraftclienthttpexception) {
                  Component component1 = this.getHttpErrorDescription(minecraftclienthttpexception);
                  throw new CompletionException(new AbuseReportSender.SendException(component1, minecraftclienthttpexception));
               } catch (MinecraftClientException minecraftclientexception) {
                  Component component = this.getErrorDescription(minecraftclientexception);
                  throw new CompletionException(new AbuseReportSender.SendException(component, minecraftclientexception));
               }
            },
            Util.ioPool()
         );
      }

      @Override
      public boolean isEnabled() {
         return this.userApiService.canSendReports();
      }

      private Component getHttpErrorDescription(MinecraftClientHttpException p_239705_) {
         return Component.translatable("gui.abuseReport.send.error_message", p_239705_.getMessage());
      }

      private Component getErrorDescription(MinecraftClientException p_240068_) {
         return switch (p_240068_.getType()) {
            case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
            case HTTP_ERROR -> HTTP_ERROR_TEXT;
            case JSON_ERROR -> JSON_ERROR_TEXT;
            default -> throw new MatchException(null, null);
         };
      }

      @Override
      public AbuseReportLimits reportLimits() {
         return this.userApiService.getAbuseReportLimits();
      }
   }
}
