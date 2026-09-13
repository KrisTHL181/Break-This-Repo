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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SkinReport extends Report {
   final Supplier<PlayerSkin> skinGetter;

   SkinReport(UUID p_298927_, Instant p_300791_, UUID p_298854_, Supplier<PlayerSkin> p_299618_) {
      super(p_298927_, p_300791_, p_298854_);
      this.skinGetter = p_299618_;
   }

   public Supplier<PlayerSkin> getSkinGetter() {
      return this.skinGetter;
   }

   public SkinReport copy() {
      SkinReport skinreport = new SkinReport(this.reportId, this.createdAt, this.reportedProfileId, this.skinGetter);
      skinreport.comments = this.comments;
      skinreport.reason = this.reason;
      skinreport.attested = this.attested;
      return skinreport;
   }

   @Override
   public Screen createScreen(Screen p_297640_, ReportingContext p_297669_) {
      return new SkinReportScreen(p_297640_, p_297669_, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder extends Report.Builder<SkinReport> {
      public Builder(SkinReport p_297260_, AbuseReportLimits p_298411_) {
         super(p_297260_, p_298411_);
      }

      public Builder(UUID p_301218_, Supplier<PlayerSkin> p_298052_, AbuseReportLimits p_299174_) {
         super(new SkinReport(UUID.randomUUID(), Instant.now(), p_301218_, p_298052_), p_299174_);
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
      public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext p_297496_) {
         Report.CannotBuildReason report$cannotbuildreason = this.checkBuildable();
         if (report$cannotbuildreason != null) {
            return Either.right(report$cannotbuildreason);
         }

         String s = Objects.requireNonNull(this.report.reason).backendName();
         ReportedEntity reportedentity = new ReportedEntity(this.report.reportedProfileId);
         PlayerSkin playerskin = this.report.skinGetter.get();
         String s1 = playerskin.body() instanceof ClientAsset.DownloadedTexture clientasset$downloadedtexture ? clientasset$downloadedtexture.url() : null;
         AbuseReport abusereport = AbuseReport.skin(this.report.comments, s, s1, reportedentity, this.report.createdAt);
         return Either.left(new Report.Result(this.report.reportId, ReportType.SKIN, abusereport));
      }
   }
}
