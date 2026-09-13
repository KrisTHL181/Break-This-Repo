package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

public class LastSeenMessagesValidator {
   private final int lastSeenCount;
   private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList();
   private @Nullable MessageSignature lastPendingMessage;

   public LastSeenMessagesValidator(final int lastSeenCount) {
      this.lastSeenCount = lastSeenCount;

      for (int i = 0; i < lastSeenCount; i++) {
         this.trackedMessages.add(null);
      }
   }

   public void addPending(final MessageSignature message) {
      if (!message.equals(this.lastPendingMessage)) {
         this.trackedMessages.add(new LastSeenTrackedEntry(message, true));
         this.lastPendingMessage = message;
      }
   }

   public int trackedMessagesCount() {
      return this.trackedMessages.size();
   }

   public void applyOffset(final int offset) throws LastSeenMessagesValidator.ValidationException {
      int maxOffset = this.trackedMessages.size() - this.lastSeenCount;
      if (offset >= 0 && offset <= maxOffset) {
         this.trackedMessages.removeElements(0, offset);
      } else {
         throw new LastSeenMessagesValidator.ValidationException("Advanced last seen window by " + offset + " messages, but expected at most " + maxOffset);
      }
   }

   public LastSeenMessages applyUpdate(final LastSeenMessages.Update update) throws LastSeenMessagesValidator.ValidationException {
      this.applyOffset(update.offset());
      ObjectList<MessageSignature> lastSeenEntries = new ObjectArrayList(update.acknowledged().cardinality());
      if (update.acknowledged().length() > this.lastSeenCount) {
         throw new LastSeenMessagesValidator.ValidationException(
            "Last seen update contained " + update.acknowledged().length() + " messages, but maximum window size is " + this.lastSeenCount
         );
      }

      for (int i = 0; i < this.lastSeenCount; i++) {
         boolean acknowledged = update.acknowledged().get(i);
         LastSeenTrackedEntry message = (LastSeenTrackedEntry)this.trackedMessages.get(i);
         if (acknowledged) {
            if (message == null) {
               throw new LastSeenMessagesValidator.ValidationException("Last seen update acknowledged unknown or previously ignored message at index " + i);
            }

            this.trackedMessages.set(i, message.acknowledge());
            lastSeenEntries.add(message.signature());
         } else {
            if (message != null && !message.pending()) {
               throw new LastSeenMessagesValidator.ValidationException(
                  "Last seen update ignored previously acknowledged message at index " + i + " and signature " + message.signature()
               );
            }

            this.trackedMessages.set(i, null);
         }
      }

      LastSeenMessages lastSeen = new LastSeenMessages(lastSeenEntries);
      if (!update.verifyChecksum(lastSeen)) {
         throw new LastSeenMessagesValidator.ValidationException("Checksum mismatch on last seen update: the client and server must have desynced");
      } else {
         return lastSeen;
      }
   }

   public static class ValidationException extends Exception {
      public ValidationException(final String message) {
         super(message);
      }
   }
}
