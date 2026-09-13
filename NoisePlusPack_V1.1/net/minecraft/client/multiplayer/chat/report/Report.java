package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class Report {
   protected final UUID reportId;
   protected final Instant createdAt;
   protected final UUID reportedProfileId;
   protected String comments = "";
   protected @Nullable ReportReason reason;
   protected boolean attested;

   public Report(UUID p_297657_, Instant p_300470_, UUID p_297764_) {
      this.reportId = p_297657_;
      this.createdAt = p_300470_;
      this.reportedProfileId = p_297764_;
   }

   public boolean isReportedPlayer(UUID p_297578_) {
      return p_297578_.equals(this.reportedProfileId);
   }

   public abstract Report copy();

   public abstract Screen createScreen(Screen var1, ReportingContext var2);

   @OnlyIn(Dist.CLIENT)
   public abstract static class Builder<R extends Report> {
      protected final R report;
      protected final AbuseReportLimits limits;

      protected Builder(R p_299684_, AbuseReportLimits p_297887_) {
         this.report = p_299684_;
         this.limits = p_297887_;
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

      public void setComments(String p_298827_) {
         this.report.comments = p_298827_;
      }

      public @Nullable ReportReason reason() {
         return this.report.reason;
      }

      public void setReason(ReportReason p_298659_) {
         this.report.reason = p_298659_;
      }

      public void setAttested(boolean p_344722_) {
         this.report.attested = p_344722_;
      }

      public abstract boolean hasContent();

      public Report.@Nullable CannotBuildReason checkBuildable() {
         return !this.report().attested ? Report.CannotBuildReason.NOT_ATTESTED : null;
      }

      public abstract Either<Report.Result, Report.CannotBuildReason> build(ReportingContext var1);
   }

   @OnlyIn(Dist.CLIENT)
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

   @OnlyIn(Dist.CLIENT)
   public record Result(UUID id, ReportType reportType, AbuseReport report) {
   }
}
