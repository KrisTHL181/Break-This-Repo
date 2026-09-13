package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ReportingContext {
   private static final int LOG_CAPACITY = 1024;
   private final AbuseReportSender sender;
   private final ReportEnvironment environment;
   private final ChatLog chatLog;
   private @Nullable Report draftReport;

   public ReportingContext(AbuseReportSender p_239187_, ReportEnvironment p_239188_, ChatLog p_239189_) {
      this.sender = p_239187_;
      this.environment = p_239188_;
      this.chatLog = p_239189_;
   }

   public static ReportingContext create(ReportEnvironment p_239686_, UserApiService p_239687_) {
      ChatLog chatlog = new ChatLog(1024);
      AbuseReportSender abusereportsender = AbuseReportSender.create(p_239686_, p_239687_);
      return new ReportingContext(abusereportsender, p_239686_, chatlog);
   }

   public void draftReportHandled(Minecraft p_261771_, Screen p_261866_, Runnable p_262031_, boolean p_261540_) {
      if (this.draftReport != null) {
         Report report = this.draftReport.copy();
         p_261771_.setScreen(
            new ConfirmScreen(
               p_296240_ -> {
                  this.setReportDraft(null);
                  if (p_296240_) {
                     p_261771_.setScreen(report.createScreen(p_261866_, this));
                  } else {
                     p_262031_.run();
                  }
               },
               Component.translatable(p_261540_ ? "gui.abuseReport.draft.quittotitle.title" : "gui.abuseReport.draft.title"),
               Component.translatable(p_261540_ ? "gui.abuseReport.draft.quittotitle.content" : "gui.abuseReport.draft.content"),
               Component.translatable("gui.abuseReport.draft.edit"),
               Component.translatable("gui.abuseReport.draft.discard")
            )
         );
      } else {
         p_262031_.run();
      }
   }

   public AbuseReportSender sender() {
      return this.sender;
   }

   public ChatLog chatLog() {
      return this.chatLog;
   }

   public boolean matches(ReportEnvironment p_239734_) {
      return Objects.equals(this.environment, p_239734_);
   }

   public void setReportDraft(@Nullable Report p_299003_) {
      this.draftReport = p_299003_;
   }

   public boolean hasDraftReport() {
      return this.draftReport != null;
   }

   public boolean hasDraftReportFor(UUID p_254340_) {
      return this.hasDraftReport() && this.draftReport.isReportedPlayer(p_254340_);
   }
}
