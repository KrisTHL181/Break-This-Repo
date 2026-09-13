package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

public class LastSeenMessagesValidator {
   private final int lastSeenCount;
   private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList();
   private @Nullable MessageSignature lastPendingMessage;

   public LastSeenMessagesValidator(int p_249951_) {
      this.lastSeenCount = p_249951_;

      for (int i = 0; i < p_249951_; i++) {
         this.trackedMessages.add(null);
      }
   }

   public void addPending(MessageSignature p_248841_) {
      if (!p_248841_.equals(this.lastPendingMessage)) {
         this.trackedMessages.add(new LastSeenTrackedEntry(p_248841_, true));
         this.lastPendingMessage = p_248841_;
      }
   }

   public int trackedMessagesCount() {
      return this.trackedMessages.size();
   }

   public void applyOffset(int p_251273_) throws LastSeenMessagesValidator.ValidationException {
      int i = this.trackedMessages.size() - this.lastSeenCount;
      if (p_251273_ >= 0 && p_251273_ <= i) {
         this.trackedMessages.removeElements(0, p_251273_);
      } else {
         throw new LastSeenMessagesValidator.ValidationException("Advanced last seen window by " + p_251273_ + " messages, but expected at most " + i);
      }
   }

   public LastSeenMessages applyUpdate(LastSeenMessages.Update p_248868_) throws LastSeenMessagesValidator.ValidationException {
      this.applyOffset(p_248868_.offset());
      ObjectList<MessageSignature> objectlist = new ObjectArrayList(p_248868_.acknowledged().cardinality());
      if (p_248868_.acknowledged().length() > this.lastSeenCount) {
         throw new LastSeenMessagesValidator.ValidationException(
            "Last seen update contained " + p_248868_.acknowledged().length() + " messages, but maximum window size is " + this.lastSeenCount
         );
      }

      for (int i = 0; i < this.lastSeenCount; i++) {
         boolean flag = p_248868_.acknowledged().get(i);
         LastSeenTrackedEntry lastseentrackedentry = (LastSeenTrackedEntry)this.trackedMessages.get(i);
         if (flag) {
            if (lastseentrackedentry == null) {
               throw new LastSeenMessagesValidator.ValidationException("Last seen update acknowledged unknown or previously ignored message at index " + i);
            }

            this.trackedMessages.set(i, lastseentrackedentry.acknowledge());
            objectlist.add(lastseentrackedentry.signature());
         } else {
            if (lastseentrackedentry != null && !lastseentrackedentry.pending()) {
               throw new LastSeenMessagesValidator.ValidationException(
                  "Last seen update ignored previously acknowledged message at index " + i + " and signature " + lastseentrackedentry.signature()
               );
            }

            this.trackedMessages.set(i, null);
         }
      }

      LastSeenMessages lastseenmessages = new LastSeenMessages(objectlist);
      if (!p_248868_.verifyChecksum(lastseenmessages)) {
         throw new LastSeenMessagesValidator.ValidationException("Checksum mismatch on last seen update: the client and server must have desynced");
      } else {
         return lastseenmessages;
      }
   }

   public static class ValidationException extends Exception {
      public ValidationException(String p_394588_) {
         super(p_394588_);
      }
   }
}
