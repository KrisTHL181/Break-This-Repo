package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;

public record PendingRpcRequest<Result>(
   Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method, CompletableFuture<Result> resultFuture, long timeoutTime
) {
   public void accept(final JsonElement response) {
      try {
         Result result = (Result)this.method.value().decodeResult(response);
         this.resultFuture.complete(Objects.requireNonNull(result));
      } catch (Exception e) {
         this.resultFuture.completeExceptionally(e);
      }
   }

   public boolean timedOut(final long currentTime) {
      return currentTime > this.timeoutTime;
   }
}
