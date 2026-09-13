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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatReport extends Report {
   final IntSet reportedMessages = new IntOpenHashSet();

   ChatReport(UUID p_298678_, Instant p_299093_, UUID p_300487_) {
      super(p_298678_, p_299093_, p_300487_);
   }

   public void toggleReported(int p_300824_, AbuseReportLimits p_301279_) {
      if (this.reportedMessages.contains(p_300824_)) {
         this.reportedMessages.remove(p_300824_);
      } else if (this.reportedMessages.size() < p_301279_.maxReportedMessageCount()) {
         this.reportedMessages.add(p_300824_);
      }
   }

   public ChatReport copy() {
      ChatReport chatreport = new ChatReport(this.reportId, this.createdAt, this.reportedProfileId);
      chatreport.reportedMessages.addAll(this.reportedMessages);
      chatreport.comments = this.comments;
      chatreport.reason = this.reason;
      chatreport.attested = this.attested;
      return chatreport;
   }

   @Override
   public Screen createScreen(Screen p_300210_, ReportingContext p_298195_) {
      return new ChatReportScreen(p_300210_, p_298195_, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder extends Report.Builder<ChatReport> {
      public Builder(ChatReport p_300891_, AbuseReportLimits p_300207_) {
         super(p_300891_, p_300207_);
      }

      public Builder(UUID p_298582_, AbuseReportLimits p_300464_) {
         super(new ChatReport(UUID.randomUUID(), Instant.now(), p_298582_), p_300464_);
      }

      public IntSet reportedMessages() {
         return this.report.reportedMessages;
      }

      public void toggleReported(int p_300108_) {
         this.report.toggleReported(p_300108_, this.limits);
      }

      public boolean isReported(int p_298529_) {
         return this.report.reportedMessages.contains(p_298529_);
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
      public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext p_298383_) {
         Report.CannotBuildReason report$cannotbuildreason = this.checkBuildable();
         if (report$cannotbuildreason != null) {
            return Either.right(report$cannotbuildreason);
         }

         String s = Objects.requireNonNull(this.report.reason).backendName();
         ReportEvidence reportevidence = this.buildEvidence(p_298383_);
         ReportedEntity reportedentity = new ReportedEntity(this.report.reportedProfileId);
         AbuseReport abusereport = AbuseReport.chat(this.report.comments, s, reportevidence, reportedentity, this.report.createdAt);
         return Either.left(new Report.Result(this.report.reportId, ReportType.CHAT, abusereport));
      }

      private ReportEvidence buildEvidence(ReportingContext p_297642_) {
         List<ReportChatMessage> list = new ArrayList<>();
         ChatReportContextBuilder chatreportcontextbuilder = new ChatReportContextBuilder(this.limits.leadingContextMessageCount());
         chatreportcontextbuilder.collectAllContext(
            p_297642_.chatLog(),
            this.report.reportedMessages,
            (p_299095_, p_300385_) -> list.add(this.buildReportedChatMessage(p_300385_, this.isReported(p_299095_)))
         );
         return new ReportEvidence(Lists.reverse(list));
      }

      private ReportChatMessage buildReportedChatMessage(LoggedChatMessage.Player p_299286_, boolean p_299614_) {
         SignedMessageLink signedmessagelink = p_299286_.message().link();
         SignedMessageBody signedmessagebody = p_299286_.message().signedBody();
         List<ByteBuffer> list = signedmessagebody.lastSeen().entries().stream().map(MessageSignature::asByteBuffer).toList();
         ByteBuffer bytebuffer = Optionull.map(p_299286_.message().signature(), MessageSignature::asByteBuffer);
         return new ReportChatMessage(
            signedmessagelink.index(),
            signedmessagelink.sender(),
            signedmessagelink.sessionId(),
            signedmessagebody.timeStamp(),
            signedmessagebody.salt(),
            list,
            signedmessagebody.content(),
            bytebuffer,
            p_299614_
         );
      }

      public ChatReport.Builder copy() {
         return new ChatReport.Builder(this.report.copy(), this.limits);
      }
   }
}
