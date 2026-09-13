package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.BitSet;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class LastSeenMessagesTracker {
   private final @Nullable LastSeenTrackedEntry[] trackedMessages;
   private int tail;
   private int offset;
   private @Nullable MessageSignature lastTrackedMessage;

   public LastSeenMessagesTracker(final int lastSeenCount) {
      this.trackedMessages = new LastSeenTrackedEntry[lastSeenCount];
   }

   public boolean addPending(final MessageSignature message, final boolean wasShown) {
      if (Objects.equals(message, this.lastTrackedMessage)) {
         return false;
      }

      this.lastTrackedMessage = message;
      this.addEntry(wasShown ? new LastSeenTrackedEntry(message, true) : null);
      return true;
   }

   private void addEntry(final @Nullable LastSeenTrackedEntry entry) {
      int index = this.tail;
      this.tail = (index + 1) % this.trackedMessages.length;
      this.offset++;
      this.trackedMessages[index] = entry;
   }

   public void ignorePending(final MessageSignature pendingMessage) {
      for (int i = 0; i < this.trackedMessages.length; i++) {
         LastSeenTrackedEntry entry = this.trackedMessages[i];
         if (entry != null && entry.pending() && pendingMessage.equals(entry.signature())) {
            this.trackedMessages[i] = null;
            break;
         }
      }
   }

   public int getAndClearOffset() {
      int originalOffset = this.offset;
      this.offset = 0;
      return originalOffset;
   }

   public LastSeenMessagesTracker.Update generateAndApplyUpdate() {
      int offset = this.getAndClearOffset();
      BitSet acknowledged = new BitSet(this.trackedMessages.length);
      ObjectList<MessageSignature> lastSeenEntries = new ObjectArrayList(this.trackedMessages.length);

      for (int i = 0; i < this.trackedMessages.length; i++) {
         int index = (this.tail + i) % this.trackedMessages.length;
         LastSeenTrackedEntry message = this.trackedMessages[index];
         if (message != null) {
            acknowledged.set(i, true);
            lastSeenEntries.add(message.signature());
            this.trackedMessages[index] = message.acknowledge();
         }
      }

      LastSeenMessages lastSeen = new LastSeenMessages(lastSeenEntries);
      LastSeenMessages.Update update = new LastSeenMessages.Update(offset, acknowledged, lastSeen.computeChecksum());
      return new LastSeenMessagesTracker.Update(lastSeen, update);
   }

   public int offset() {
      return this.offset;
   }

   public record Update(LastSeenMessages lastSeen, LastSeenMessages.Update update) {
   }
}
