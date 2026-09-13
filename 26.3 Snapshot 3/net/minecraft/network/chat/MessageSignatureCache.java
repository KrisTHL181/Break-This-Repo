package net.minecraft.network.chat;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class MessageSignatureCache {
   public static final int NOT_FOUND = -1;
   private static final int DEFAULT_CAPACITY = 128;
   private final @Nullable MessageSignature[] entries;

   public MessageSignatureCache(final int capacity) {
      this.entries = new MessageSignature[capacity];
   }

   public static MessageSignatureCache createDefault() {
      return new MessageSignatureCache(128);
   }

   public int pack(final MessageSignature signature) {
      for (int i = 0; i < this.entries.length; i++) {
         if (signature.equals(this.entries[i])) {
            return i;
         }
      }

      return -1;
   }

   public @Nullable MessageSignature unpack(final int id) {
      return this.entries[id];
   }

   public void push(final SignedMessageBody body, final @Nullable MessageSignature signature) {
      List<MessageSignature> lastSeen = body.lastSeen().entries();
      ArrayDeque<MessageSignature> queue = new ArrayDeque<>(lastSeen.size() + 1);
      queue.addAll(lastSeen);
      if (signature != null) {
         queue.add(signature);
      }

      this.push(queue);
   }

   @VisibleForTesting
   void push(final List<MessageSignature> entries) {
      this.push(new ArrayDeque<>(entries));
   }

   private void push(final ArrayDeque<MessageSignature> queue) {
      Set<MessageSignature> newEntries = new ObjectOpenHashSet(queue);

      for (int i = 0; !queue.isEmpty() && i < this.entries.length; i++) {
         MessageSignature entry = this.entries[i];
         this.entries[i] = queue.removeLast();
         if (entry != null && !newEntries.contains(entry)) {
            queue.addFirst(entry);
         }
      }
   }
}
