package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;

public record PendingRpcRequest<Result>(
   Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method, CompletableFuture<Result> resultFuture, long timeoutTime
) {
   public void accept(JsonElement p_423259_) {
      try {
         Result result = (Result)this.method.value().decodeResult(p_423259_);
         this.resultFuture.complete(Objects.requireNonNull(result));
      } catch (Exception exception) {
         this.resultFuture.completeExceptionally(exception);
      }
   }

   public boolean timedOut(long p_422494_) {
      return p_422494_ > this.timeoutTime;
   }
}
