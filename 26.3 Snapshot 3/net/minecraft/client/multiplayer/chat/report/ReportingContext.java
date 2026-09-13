package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class ReportingContext {
   private static final int LOG_CAPACITY = 1024;
   private final AbuseReportSender sender;
   private final ReportEnvironment environment;
   private final ChatLog chatLog;
   private @Nullable Report draftReport;

   public ReportingContext(final AbuseReportSender sender, final ReportEnvironment environment, final ChatLog chatLog) {
      this.sender = sender;
      this.environment = environment;
      this.chatLog = chatLog;
   }

   public static ReportingContext create(final ReportEnvironment environment, final UserApiService userApiService) {
      ChatLog chatLog = new ChatLog(1024);
      AbuseReportSender sender = AbuseReportSender.create(environment, userApiService);
      return new ReportingContext(sender, environment, chatLog);
   }

   public void draftReportHandled(final Minecraft minecraft, final Screen lastScreen, final Runnable onDiscard, final boolean quitToTitle) {
      if (this.draftReport != null) {
         Report report = this.draftReport.copy();
         minecraft.gui
            .setScreen(
               new ConfirmScreen(
                  response -> {
                     this.setReportDraft(null);
                     if (response) {
                        minecraft.gui.setScreen(report.createScreen(lastScreen, this));
                     } else {
                        onDiscard.run();
                     }
                  },
                  Component.translatable(quitToTitle ? "gui.abuseReport.draft.quittotitle.title" : "gui.abuseReport.draft.title"),
                  Component.translatable(quitToTitle ? "gui.abuseReport.draft.quittotitle.content" : "gui.abuseReport.draft.content"),
                  Component.translatable("gui.abuseReport.draft.edit"),
                  Component.translatable("gui.abuseReport.draft.discard")
               )
            );
      } else {
         onDiscard.run();
      }
   }

   public AbuseReportSender sender() {
      return this.sender;
   }

   public ChatLog chatLog() {
      return this.chatLog;
   }

   public boolean matches(final ReportEnvironment environment) {
      return Objects.equals(this.environment, environment);
   }

   public void setReportDraft(final @Nullable Report draftReport) {
      this.draftReport = draftReport;
   }

   public boolean hasDraftReport() {
      return this.draftReport != null;
   }

   public boolean hasDraftReportFor(final UUID playerId) {
      return this.hasDraftReport() && this.draftReport.isReportedPlayer(playerId);
   }
}
