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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionLogFiller {
   private final ChatLog log;
   private final ChatReportContextBuilder contextBuilder;
   private final Predicate<LoggedChatMessage.Player> canReport;
   private @Nullable SignedMessageLink previousLink = null;
   private int eventId;
   private int missedCount;
   private @Nullable PlayerChatMessage lastMessage;

   public ChatSelectionLogFiller(ReportingContext p_251076_, Predicate<LoggedChatMessage.Player> p_250367_) {
      this.log = p_251076_.chatLog();
      this.contextBuilder = new ChatReportContextBuilder(p_251076_.sender().reportLimits().leadingContextMessageCount());
      this.canReport = p_250367_;
      this.eventId = this.log.end();
   }

   public void fillNextPage(int p_239016_, ChatSelectionLogFiller.Output p_239017_) {
      int i = 0;

      while (i < p_239016_) {
         LoggedChatEvent loggedchatevent = this.log.lookup(this.eventId);
         if (loggedchatevent == null) {
            break;
         }

         int j = this.eventId--;
         if (loggedchatevent instanceof LoggedChatMessage.Player loggedchatmessage$player && !loggedchatmessage$player.message().equals(this.lastMessage)) {
            if (this.acceptMessage(p_239017_, loggedchatmessage$player)) {
               if (this.missedCount > 0) {
                  p_239017_.acceptDivider(Component.translatable("gui.chatSelection.fold", this.missedCount));
                  this.missedCount = 0;
               }

               p_239017_.acceptMessage(j, loggedchatmessage$player);
               i++;
            } else {
               this.missedCount++;
            }

            this.lastMessage = loggedchatmessage$player.message();
         }
      }
   }

   private boolean acceptMessage(ChatSelectionLogFiller.Output p_254300_, LoggedChatMessage.Player p_253803_) {
      PlayerChatMessage playerchatmessage = p_253803_.message();
      boolean flag = this.contextBuilder.acceptContext(playerchatmessage);
      if (this.canReport.test(p_253803_)) {
         this.contextBuilder.trackContext(playerchatmessage);
         if (this.previousLink != null && !this.previousLink.isDescendantOf(playerchatmessage.link())) {
            p_254300_.acceptDivider(Component.translatable("gui.chatSelection.join", p_253803_.profile().name()).withStyle(ChatFormatting.YELLOW));
         }

         this.previousLink = playerchatmessage.link();
         return true;
      } else {
         return flag;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Output {
      void acceptMessage(int var1, LoggedChatMessage.Player var2);

      void acceptDivider(Component var1);
   }
}
