package net.minecraft.client.gui.screens.reporting;

import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportContextBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageLink;
import org.jspecify.annotations.Nullable;

public class ChatSelectionLogFiller {
   private final ChatLog log;
   private final ChatReportContextBuilder contextBuilder;
   private final Predicate<LoggedChatMessage.Player> canReport;
   private @Nullable SignedMessageLink previousLink = null;
   private int eventId;
   private int missedCount;
   private @Nullable PlayerChatMessage lastMessage;

   public ChatSelectionLogFiller(final ReportingContext reportingContext, final Predicate<LoggedChatMessage.Player> canReport) {
      this.log = reportingContext.chatLog();
      this.contextBuilder = new ChatReportContextBuilder(reportingContext.sender().reportLimits().leadingContextMessageCount());
      this.canReport = canReport;
      this.eventId = this.log.end();
   }

   public void fillNextPage(final int pageSize, final ChatSelectionLogFiller.Output output) {
      int count = 0;

      while (count < pageSize) {
         LoggedChatEvent event = this.log.lookup(this.eventId);
         if (event == null) {
            break;
         }

         int eventId = this.eventId--;
         if (event instanceof LoggedChatMessage.Player message && !message.message().equals(this.lastMessage)) {
            if (this.acceptMessage(output, message)) {
               if (this.missedCount > 0) {
                  output.acceptDivider(Component.translatable("gui.chatSelection.fold", this.missedCount));
                  this.missedCount = 0;
               }

               output.acceptMessage(eventId, message);
               count++;
            } else {
               this.missedCount++;
            }

            this.lastMessage = message.message();
         }
      }
   }

   private boolean acceptMessage(final ChatSelectionLogFiller.Output output, final LoggedChatMessage.Player event) {
      PlayerChatMessage message = event.message();
      boolean context = this.contextBuilder.acceptContext(message);
      if (this.canReport.test(event)) {
         this.contextBuilder.trackContext(message);
         if (this.previousLink != null && !this.previousLink.isDescendantOf(message.link())) {
            output.acceptDivider(Component.translatable("gui.chatSelection.join", event.profile().name()).withStyle(ChatFormatting.YELLOW));
         }

         this.previousLink = message.link();
         return true;
      } else {
         return context;
      }
   }

   public interface Output {
      void acceptMessage(int id, LoggedChatMessage.Player message);

      void acceptDivider(Component text);
   }
}
