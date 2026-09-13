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

   public LastSeenMessagesTracker(int p_242388_) {
      this.trackedMessages = new LastSeenTrackedEntry[p_242388_];
   }

   public boolean addPending(MessageSignature p_248926_, boolean p_250312_) {
      if (Objects.equals(p_248926_, this.lastTrackedMessage)) {
         return false;
      }

      this.lastTrackedMessage = p_248926_;
      this.addEntry(p_250312_ ? new LastSeenTrackedEntry(p_248926_, true) : null);
      return true;
   }

   private void addEntry(@Nullable LastSeenTrackedEntry p_250255_) {
      int i = this.tail;
      this.tail = (i + 1) % this.trackedMessages.length;
      this.offset++;
      this.trackedMessages[i] = p_250255_;
   }

   public void ignorePending(MessageSignature p_251020_) {
      for (int i = 0; i < this.trackedMessages.length; i++) {
         LastSeenTrackedEntry lastseentrackedentry = this.trackedMessages[i];
         if (lastseentrackedentry != null && lastseentrackedentry.pending() && p_251020_.equals(lastseentrackedentry.signature())) {
            this.trackedMessages[i] = null;
            break;
         }
      }
   }

   public int getAndClearOffset() {
      int i = this.offset;
      this.offset = 0;
      return i;
   }

   public LastSeenMessagesTracker.Update generateAndApplyUpdate() {
      int i = this.getAndClearOffset();
      BitSet bitset = new BitSet(this.trackedMessages.length);
      ObjectList<MessageSignature> objectlist = new ObjectArrayList(this.trackedMessages.length);

      for (int j = 0; j < this.trackedMessages.length; j++) {
         int k = (this.tail + j) % this.trackedMessages.length;
         LastSeenTrackedEntry lastseentrackedentry = this.trackedMessages[k];
         if (lastseentrackedentry != null) {
            bitset.set(j, true);
            objectlist.add(lastseentrackedentry.signature());
            this.trackedMessages[k] = lastseentrackedentry.acknowledge();
         }
      }

      LastSeenMessages lastseenmessages = new LastSeenMessages(objectlist);
      LastSeenMessages.Update lastseenmessages$update = new LastSeenMessages.Update(i, bitset, lastseenmessages.computeChecksum());
      return new LastSeenMessagesTracker.Update(lastseenmessages, lastseenmessages$update);
   }

   public int offset() {
      return this.offset;
   }

   public record Update(LastSeenMessages lastSeen, LastSeenMessages.Update update) {
   }
}
