package net.minecraft.client.multiplayer.chat.report;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatReportContextBuilder {
   final int leadingCount;
   private final List<ChatReportContextBuilder.Collector> activeCollectors = new ArrayList<>();

   public ChatReportContextBuilder(int p_252198_) {
      this.leadingCount = p_252198_;
   }

   public void collectAllContext(ChatLog p_249467_, IntCollection p_250295_, ChatReportContextBuilder.Handler p_251946_) {
      IntSortedSet intsortedset = new IntRBTreeSet(p_250295_);

      for (int i = intsortedset.lastInt(); i >= p_249467_.start() && (this.isActive() || !intsortedset.isEmpty()); i--) {
         if (p_249467_.lookup(i) instanceof LoggedChatMessage.Player loggedchatmessage$player) {
            boolean flag = this.acceptContext(loggedchatmessage$player.message());
            if (intsortedset.remove(i)) {
               this.trackContext(loggedchatmessage$player.message());
               p_251946_.accept(i, loggedchatmessage$player);
            } else if (flag) {
               p_251946_.accept(i, loggedchatmessage$player);
            }
         }
      }
   }

   public void trackContext(PlayerChatMessage p_252057_) {
      this.activeCollectors.add(new ChatReportContextBuilder.Collector(p_252057_));
   }

   public boolean acceptContext(PlayerChatMessage p_250059_) {
      boolean flag = false;
      Iterator<ChatReportContextBuilder.Collector> iterator = this.activeCollectors.iterator();

      while (iterator.hasNext()) {
         ChatReportContextBuilder.Collector chatreportcontextbuilder$collector = iterator.next();
         if (chatreportcontextbuilder$collector.accept(p_250059_)) {
            flag = true;
            if (chatreportcontextbuilder$collector.isComplete()) {
               iterator.remove();
            }
         }
      }

      return flag;
   }

   public boolean isActive() {
      return !this.activeCollectors.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   class Collector {
      private final Set<MessageSignature> lastSeenSignatures;
      private PlayerChatMessage lastChainMessage;
      private boolean collectingChain = true;
      private int count;

      Collector(final PlayerChatMessage p_249708_) {
         this.lastSeenSignatures = new ObjectOpenHashSet(p_249708_.signedBody().lastSeen().entries());
         this.lastChainMessage = p_249708_;
      }

      boolean accept(PlayerChatMessage p_252313_) {
         if (p_252313_.equals(this.lastChainMessage)) {
            return false;
         }

         boolean flag = this.lastSeenSignatures.remove(p_252313_.signature());
         if (this.collectingChain && this.lastChainMessage.sender().equals(p_252313_.sender())) {
            if (this.lastChainMessage.link().isDescendantOf(p_252313_.link())) {
               flag = true;
               this.lastChainMessage = p_252313_;
            } else {
               this.collectingChain = false;
            }
         }

         if (flag) {
            this.count++;
         }

         return flag;
      }

      boolean isComplete() {
         return this.count >= ChatReportContextBuilder.this.leadingCount || !this.collectingChain && this.lastSeenSignatures.isEmpty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Handler {
      void accept(int var1, LoggedChatMessage.Player var2);
   }
}
