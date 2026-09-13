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

public class ChatReportContextBuilder {
   private final int leadingCount;
   private final List<ChatReportContextBuilder.Collector> activeCollectors = new ArrayList<>();

   public ChatReportContextBuilder(final int leadingCount) {
      this.leadingCount = leadingCount;
   }

   public void collectAllContext(final ChatLog chatLog, final IntCollection roots, final ChatReportContextBuilder.Handler handler) {
      IntSortedSet uncollectedRoots = new IntRBTreeSet(roots);

      for (int id = uncollectedRoots.lastInt(); id >= chatLog.start() && (this.isActive() || !uncollectedRoots.isEmpty()); id--) {
         if (chatLog.lookup(id) instanceof LoggedChatMessage.Player event) {
            boolean context = this.acceptContext(event.message());
            if (uncollectedRoots.remove(id)) {
               this.trackContext(event.message());
               handler.accept(id, event);
            } else if (context) {
               handler.accept(id, event);
            }
         }
      }
   }

   public void trackContext(final PlayerChatMessage message) {
      this.activeCollectors.add(new ChatReportContextBuilder.Collector(message));
   }

   public boolean acceptContext(final PlayerChatMessage message) {
      boolean collected = false;
      Iterator<ChatReportContextBuilder.Collector> iterator = this.activeCollectors.iterator();

      while (iterator.hasNext()) {
         ChatReportContextBuilder.Collector collector = iterator.next();
         if (collector.accept(message)) {
            collected = true;
            if (collector.isComplete()) {
               iterator.remove();
            }
         }
      }

      return collected;
   }

   public boolean isActive() {
      return !this.activeCollectors.isEmpty();
   }

   private class Collector {
      private final Set<MessageSignature> lastSeenSignatures;
      private PlayerChatMessage lastChainMessage;
      private boolean collectingChain = true;
      private int count;

      private Collector(final PlayerChatMessage fromMessage) {
         this.lastSeenSignatures = new ObjectOpenHashSet(fromMessage.signedBody().lastSeen().entries());
         this.lastChainMessage = fromMessage;
      }

      private boolean accept(final PlayerChatMessage message) {
         if (message.equals(this.lastChainMessage)) {
            return false;
         }

         boolean selected = this.lastSeenSignatures.remove(message.signature());
         if (this.collectingChain && this.lastChainMessage.sender().equals(message.sender())) {
            if (this.lastChainMessage.link().isDescendantOf(message.link())) {
               selected = true;
               this.lastChainMessage = message;
            } else {
               this.collectingChain = false;
            }
         }

         if (selected) {
            this.count++;
         }

         return selected;
      }

      private boolean isComplete() {
         return this.count >= ChatReportContextBuilder.this.leadingCount || !this.collectingChain && this.lastSeenSignatures.isEmpty();
      }
   }

   public interface Handler {
      void accept(int id, LoggedChatMessage.Player event);
   }
}
