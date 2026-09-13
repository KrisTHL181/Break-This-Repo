package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.NameReportScreen;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class NameReport extends Report {
   private final String reportedName;

   private NameReport(final UUID reportId, final Instant createdAt, final UUID reportedProfileId, final String reportedName) {
      super(reportId, createdAt, reportedProfileId);
      this.reportedName = reportedName;
   }

   public String getReportedName() {
      return this.reportedName;
   }

   public NameReport copy() {
      NameReport result = new NameReport(this.reportId, this.createdAt, this.reportedProfileId, this.reportedName);
      result.comments = this.comments;
      result.attested = this.attested;
      return result;
   }

   @Override
   public Screen createScreen(final Screen lastScreen, final ReportingContext context) {
      return new NameReportScreen(lastScreen, context, this);
   }

   public static class Builder extends Report.Builder<NameReport> {
      public Builder(final NameReport report, final AbuseReportLimits limits) {
         super(report, limits);
      }

      public Builder(final UUID reportedProfileId, final String reportedName, final AbuseReportLimits limits) {
         super(new NameReport(UUID.randomUUID(), Instant.now(), reportedProfileId, reportedName), limits);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments());
      }

      @Override
      public Report.@Nullable CannotBuildReason checkBuildable() {
         return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
      }

      @Override
      public Either<Report.Result, Report.CannotBuildReason> build(final ReportingContext reportingContext) {
         Report.CannotBuildReason error = this.checkBuildable();
         if (error != null) {
            return Either.right(error);
         }

         ReportedEntity reportedEntity = new ReportedEntity(this.report.reportedProfileId);
         AbuseReport abuseReport = AbuseReport.name(this.report.comments, reportedEntity, this.report.createdAt);
         return Either.left(new Report.Result(this.report.reportId, ReportType.USERNAME, abuseReport));
      }
   }
}
