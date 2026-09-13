package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.services.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.Util;

public interface AbuseReportSender {
   static AbuseReportSender create(final ReportEnvironment environment, final UserApiService userApiService) {
      return new AbuseReportSender.Services(environment, userApiService);
   }

   CompletableFuture<Unit> send(UUID id, ReportType reportType, AbuseReport report);

   boolean isEnabled();

   default AbuseReportLimits reportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }

   class SendException extends ThrowingComponent {
      public SendException(final Component component, final Throwable cause) {
         super(component, cause);
      }
   }

   record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
      private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
      private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
      private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

      @Override
      public CompletableFuture<Unit> send(final UUID id, final ReportType reportType, final AbuseReport report) {
         return CompletableFuture.supplyAsync(
            () -> {
               AbuseReportRequest request = new AbuseReportRequest(
                  1, id, report, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo(), reportType.backendName()
               );

               try {
                  this.userApiService.reportAbuse(request);
                  return Unit.INSTANCE;
               } catch (MinecraftClientHttpException e) {
                  Component description = this.getHttpErrorDescription(e);
                  throw new CompletionException(new AbuseReportSender.SendException(description, e));
               } catch (MinecraftClientException e) {
                  Component description = this.getErrorDescription(e);
                  throw new CompletionException(new AbuseReportSender.SendException(description, e));
               }
            },
            Util.ioPool()
         );
      }

      @Override
      public boolean isEnabled() {
         return this.userApiService.canSendReports();
      }

      private Component getHttpErrorDescription(final MinecraftClientHttpException e) {
         return Component.translatable("gui.abuseReport.send.error_message", e.getMessage());
      }

      private Component getErrorDescription(final MinecraftClientException e) {
         return switch (e.getType()) {
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
