package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.SkinReportScreen;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class SkinReport extends Report {
   private final Supplier<PlayerSkin> skinGetter;

   private SkinReport(final UUID reportId, final Instant createdAt, final UUID reportedProfileId, final Supplier<PlayerSkin> skinGetter) {
      super(reportId, createdAt, reportedProfileId);
      this.skinGetter = skinGetter;
   }

   public Supplier<PlayerSkin> getSkinGetter() {
      return this.skinGetter;
   }

   public SkinReport copy() {
      SkinReport result = new SkinReport(this.reportId, this.createdAt, this.reportedProfileId, this.skinGetter);
      result.comments = this.comments;
      result.reason = this.reason;
      result.attested = this.attested;
      return result;
   }

   @Override
   public Screen createScreen(final Screen lastScreen, final ReportingContext context) {
      return new SkinReportScreen(lastScreen, context, this);
   }

   public static class Builder extends Report.Builder<SkinReport> {
      public Builder(final SkinReport report, final AbuseReportLimits limits) {
         super(report, limits);
      }

      public Builder(final UUID reportedProfileId, final Supplier<PlayerSkin> skin, final AbuseReportLimits limits) {
         super(new SkinReport(UUID.randomUUID(), Instant.now(), reportedProfileId, skin), limits);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments()) || this.reason() != null;
      }

      @Override
      public Report.@Nullable CannotBuildReason checkBuildable() {
         if (this.report.reason == null) {
            return Report.CannotBuildReason.NO_REASON;
         } else {
            return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
         }
      }

      @Override
      public Either<Report.Result, Report.CannotBuildReason> build(final ReportingContext reportingContext) {
         Report.CannotBuildReason error = this.checkBuildable();
         if (error != null) {
            return Either.right(error);
         }

         String reason = Objects.requireNonNull(this.report.reason).backendName();
         ReportedEntity reportedEntity = new ReportedEntity(this.report.reportedProfileId);
         PlayerSkin skin = this.report.skinGetter.get();
         String skinUrl = skin.body() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
         AbuseReport abuseReport = AbuseReport.skin(this.report.comments, reason, skinUrl, reportedEntity, this.report.createdAt);
         return Either.left(new Report.Result(this.report.reportId, ReportType.SKIN, abuseReport));
      }
   }
}
