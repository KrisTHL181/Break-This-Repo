package net.minecraft.client.multiplayer.chat.report;

import com.google.common.collect.Lists;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.Optionull;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class ChatReport extends Report {
   private final IntSet reportedMessages = new IntOpenHashSet();

   private ChatReport(final UUID reportId, final Instant createdAt, final UUID reportedProfileId) {
      super(reportId, createdAt, reportedProfileId);
   }

   public void toggleReported(final int id, final AbuseReportLimits limits) {
      if (this.reportedMessages.contains(id)) {
         this.reportedMessages.remove(id);
      } else if (this.reportedMessages.size() < limits.maxReportedMessageCount()) {
         this.reportedMessages.add(id);
      }
   }

   public ChatReport copy() {
      ChatReport result = new ChatReport(this.reportId, this.createdAt, this.reportedProfileId);
      result.reportedMessages.addAll(this.reportedMessages);
      result.comments = this.comments;
      result.reason = this.reason;
      result.attested = this.attested;
      return result;
   }

   @Override
   public Screen createScreen(final Screen lastScreen, final ReportingContext context) {
      return new ChatReportScreen(lastScreen, context, this);
   }

   public static class Builder extends Report.Builder<ChatReport> {
      public Builder(final ChatReport report, final AbuseReportLimits limits) {
         super(report, limits);
      }

      public Builder(final UUID reportedProfileId, final AbuseReportLimits limits) {
         super(new ChatReport(UUID.randomUUID(), Instant.now(), reportedProfileId), limits);
      }

      public IntSet reportedMessages() {
         return this.report.reportedMessages;
      }

      public void toggleReported(final int id) {
         this.report.toggleReported(id, this.limits);
      }

      public boolean isReported(final int id) {
         return this.report.reportedMessages.contains(id);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments()) || !this.reportedMessages().isEmpty() || this.reason() != null;
      }

      @Override
      public Report.@Nullable CannotBuildReason checkBuildable() {
         if (this.report.reportedMessages.isEmpty()) {
            return Report.CannotBuildReason.NO_REPORTED_MESSAGES;
         } else if (this.report.reportedMessages.size() > this.limits.maxReportedMessageCount()) {
            return Report.CannotBuildReason.TOO_MANY_MESSAGES;
         } else if (this.report.reason == null) {
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
         ReportEvidence evidence = this.buildEvidence(reportingContext);
         ReportedEntity reportedEntity = new ReportedEntity(this.report.reportedProfileId);
         AbuseReport abuseReport = AbuseReport.chat(this.report.comments, reason, evidence, reportedEntity, this.report.createdAt);
         return Either.left(new Report.Result(this.report.reportId, ReportType.CHAT, abuseReport));
      }

      private ReportEvidence buildEvidence(final ReportingContext reportingContext) {
         List<ReportChatMessage> allReportMessages = new ArrayList<>();
         ChatReportContextBuilder contextBuilder = new ChatReportContextBuilder(this.limits.leadingContextMessageCount());
         contextBuilder.collectAllContext(
            reportingContext.chatLog(),
            this.report.reportedMessages,
            (id, event) -> allReportMessages.add(this.buildReportedChatMessage(event, this.isReported(id)))
         );
         return new ReportEvidence(Lists.reverse(allReportMessages));
      }

      private ReportChatMessage buildReportedChatMessage(final LoggedChatMessage.Player chat, final boolean reported) {
         SignedMessageLink link = chat.message().link();
         SignedMessageBody body = chat.message().signedBody();
         List<ByteBuffer> lastSeen = body.lastSeen().entries().stream().map(MessageSignature::asByteBuffer).toList();
         ByteBuffer signature = Optionull.map(chat.message().signature(), MessageSignature::asByteBuffer);
         return new ReportChatMessage(
            link.index(), link.sender(), link.sessionId(), body.timeStamp(), body.salt(), lastSeen, body.content(), signature, reported
         );
      }

      public ChatReport.Builder copy() {
         return new ChatReport.Builder(this.report.copy(), this.limits);
      }
   }
}
