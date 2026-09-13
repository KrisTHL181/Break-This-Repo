package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class Report {
   protected final UUID reportId;
   protected final Instant createdAt;
   protected final UUID reportedProfileId;
   protected String comments = "";
   protected @Nullable ReportReason reason;
   protected boolean attested;

   public Report(final UUID reportId, final Instant createdAt, final UUID reportedProfileId) {
      this.reportId = reportId;
      this.createdAt = createdAt;
      this.reportedProfileId = reportedProfileId;
   }

   public boolean isReportedPlayer(final UUID playerId) {
      return playerId.equals(this.reportedProfileId);
   }

   public abstract Report copy();

   public abstract Screen createScreen(Screen lastScreen, ReportingContext context);

   public abstract static class Builder<R extends Report> {
      protected final R report;
      protected final AbuseReportLimits limits;

      protected Builder(final R report, final AbuseReportLimits limits) {
         this.report = report;
         this.limits = limits;
      }

      public R report() {
         return this.report;
      }

      public UUID reportedProfileId() {
         return this.report.reportedProfileId;
      }

      public String comments() {
         return this.report.comments;
      }

      public boolean attested() {
         return this.report().attested;
      }

      public void setComments(final String comments) {
         this.report.comments = comments;
      }

      public @Nullable ReportReason reason() {
         return this.report.reason;
      }

      public void setReason(final ReportReason reason) {
         this.report.reason = reason;
      }

      public void setAttested(final boolean attested) {
         this.report.attested = attested;
      }

      public abstract boolean hasContent();

      public Report.@Nullable CannotBuildReason checkBuildable() {
         return !this.report().attested ? Report.CannotBuildReason.NOT_ATTESTED : null;
      }

      public abstract Either<Report.Result, Report.CannotBuildReason> build(ReportingContext reportingContext);
   }

   public record CannotBuildReason(Component message) {
      public static final Report.CannotBuildReason NO_REASON = new Report.CannotBuildReason(Component.translatable("gui.abuseReport.send.no_reason"));
      public static final Report.CannotBuildReason NO_REPORTED_MESSAGES = new Report.CannotBuildReason(
         Component.translatable("gui.chatReport.send.no_reported_messages")
      );
      public static final Report.CannotBuildReason TOO_MANY_MESSAGES = new Report.CannotBuildReason(
         Component.translatable("gui.chatReport.send.too_many_messages")
      );
      public static final Report.CannotBuildReason COMMENT_TOO_LONG = new Report.CannotBuildReason(
         Component.translatable("gui.abuseReport.send.comment_too_long")
      );
      public static final Report.CannotBuildReason NOT_ATTESTED = new Report.CannotBuildReason(Component.translatable("gui.abuseReport.send.not_attested"));

      public Tooltip tooltip() {
         return Tooltip.create(this.message);
      }
   }

   public record Result(UUID id, ReportType reportType, AbuseReport report) {
   }
}
