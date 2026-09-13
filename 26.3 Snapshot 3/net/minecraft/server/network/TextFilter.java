package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TextFilter {
   TextFilter DUMMY = new TextFilter() {
      @Override
      public CompletableFuture<FilteredText> processStreamMessage(final String message) {
         return CompletableFuture.completedFuture(FilteredText.passThrough(message));
      }

      @Override
      public CompletableFuture<List<FilteredText>> processMessageBundle(final List<String> messages) {
         return CompletableFuture.completedFuture(messages.stream().map(FilteredText::passThrough).collect(ImmutableList.toImmutableList()));
      }
   };

   default void join() {
   }

   default void leave() {
   }

   CompletableFuture<FilteredText> processStreamMessage(String message);

   CompletableFuture<List<FilteredText>> processMessageBundle(List<String> messages);
}
