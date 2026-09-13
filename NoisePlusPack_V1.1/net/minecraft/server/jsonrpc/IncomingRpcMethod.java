package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import org.jspecify.annotations.Nullable;

public interface IncomingRpcMethod<Params, Result> {
   MethodInfo<Params, Result> info();

   IncomingRpcMethod.Attributes attributes();

   JsonElement apply(MinecraftApi var1, @Nullable JsonElement var2, ClientInfo var3);

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> p_423575_) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(p_423575_);
   }

   static <Params, Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> method(IncomingRpcMethod.RpcMethodFunction<Params, Result> p_427772_) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(p_427772_);
   }

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(Function<MinecraftApi, Result> p_426897_) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(p_426897_);
   }

   record Attributes(boolean runOnMainThread, boolean discoverable) {
   }

   class IncomingRpcMethodBuilder<Params, Result> {
      private String description = "";
      private @Nullable ParamInfo<Params> paramInfo;
      private @Nullable ResultInfo<Result> resultInfo;
      private boolean discoverable = true;
      private boolean runOnMainThread = true;
      private IncomingRpcMethod.@Nullable ParameterlessRpcMethodFunction<Result> parameterlessFunction;
      private IncomingRpcMethod.@Nullable RpcMethodFunction<Params, Result> parameterFunction;

      public IncomingRpcMethodBuilder(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> p_450437_) {
         this.parameterlessFunction = p_450437_;
      }

      public IncomingRpcMethodBuilder(IncomingRpcMethod.RpcMethodFunction<Params, Result> p_451290_) {
         this.parameterFunction = p_451290_;
      }

      public IncomingRpcMethodBuilder(Function<MinecraftApi, Result> p_455964_) {
         this.parameterlessFunction = (p_455727_, p_456818_) -> p_455964_.apply(p_455727_);
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> description(String p_429728_) {
         this.description = p_429728_;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> response(String p_452506_, Schema<Result> p_456361_) {
         this.resultInfo = new ResultInfo<>(p_452506_, p_456361_.info());
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> param(String p_460022_, Schema<Params> p_457026_) {
         this.paramInfo = new ParamInfo<>(p_460022_, p_457026_.info());
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> undiscoverable() {
         this.discoverable = false;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> notOnMainThread() {
         this.runOnMainThread = false;
         return this;
      }

      public IncomingRpcMethod<Params, Result> build() {
         if (this.resultInfo == null) {
            throw new IllegalStateException("No response defined");
         }

         IncomingRpcMethod.Attributes incomingrpcmethod$attributes = new IncomingRpcMethod.Attributes(this.runOnMainThread, this.discoverable);
         MethodInfo<Params, Result> methodinfo = new MethodInfo<>(this.description, this.paramInfo, this.resultInfo);
         if (this.parameterlessFunction != null) {
            return new IncomingRpcMethod.ParameterlessMethod<>(methodinfo, incomingrpcmethod$attributes, this.parameterlessFunction);
         }

         if (this.parameterFunction != null) {
            if (this.paramInfo == null) {
               throw new IllegalStateException("No param schema defined");
            } else {
               return new IncomingRpcMethod.Method<>(methodinfo, incomingrpcmethod$attributes, this.parameterFunction);
            }
         } else {
            throw new IllegalStateException("No method defined");
         }
      }

      public IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> p_426192_, String p_428528_) {
         return this.register(p_426192_, Identifier.withDefaultNamespace(p_428528_));
      }

      private IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> p_424076_, Identifier p_450798_) {
         return Registry.register(p_424076_, p_450798_, this.build());
      }
   }

   record Method<Params, Result>(
      MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.RpcMethodFunction<Params, Result> function
   ) implements IncomingRpcMethod<Params, Result> {
      @Override
      public JsonElement apply(MinecraftApi p_429665_, @Nullable JsonElement p_424548_, ClientInfo p_427391_) {
         if (p_424548_ != null && (p_424548_.isJsonArray() || p_424548_.isJsonObject())) {
            if (this.info.params().isEmpty()) {
               throw new IllegalArgumentException("Method defined as having parameters without describing them");
            }

            JsonElement jsonelement;
            if (p_424548_.isJsonObject()) {
               String s = this.info.params().get().name();
               JsonElement jsonelement1 = p_424548_.getAsJsonObject().get(s);
               if (jsonelement1 == null) {
                  throw new InvalidParameterJsonRpcException(String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", s));
               }

               jsonelement = jsonelement1;
            } else {
               JsonArray jsonarray = p_424548_.getAsJsonArray();
               if (jsonarray.isEmpty() || jsonarray.size() > 1) {
                  throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
               }

               jsonelement = jsonarray.get(0);
            }

            Params params = (Params)this.info
               .params()
               .get()
               .schema()
               .codec()
               .parse(JsonOps.INSTANCE, jsonelement)
               .getOrThrow(InvalidParameterJsonRpcException::new);
            Result result = this.function.apply(p_429665_, params, p_427391_);
            if (this.info.result().isEmpty()) {
               throw new IllegalStateException("No result codec defined");
            } else {
               return (JsonElement)this.info.result().get().schema().codec().encodeStart(JsonOps.INSTANCE, result).getOrThrow(EncodeJsonRpcException::new);
            }
         } else {
            throw new InvalidParameterJsonRpcException("Expected params as array or named");
         }
      }
   }

   record ParameterlessMethod<Params, Result>(
      MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier
   ) implements IncomingRpcMethod<Params, Result> {
      @Override
      public JsonElement apply(MinecraftApi p_430683_, @Nullable JsonElement p_427524_, ClientInfo p_423407_) {
         if (p_427524_ == null || p_427524_.isJsonArray() && p_427524_.getAsJsonArray().isEmpty()) {
            if (this.info.params().isPresent()) {
               throw new IllegalArgumentException("Parameterless method unexpectedly has parameter description");
            } else {
               Result result = this.supplier.apply(p_430683_, p_423407_);
               if (this.info.result().isEmpty()) {
                  throw new IllegalStateException("No result codec defined");
               } else {
                  return (JsonElement)this.info
                     .result()
                     .get()
                     .schema()
                     .codec()
                     .encodeStart(JsonOps.INSTANCE, result)
                     .getOrThrow(InvalidParameterJsonRpcException::new);
               }
            }
         } else {
            throw new InvalidParameterJsonRpcException("Expected no params, or an empty array");
         }
      }
   }

   @FunctionalInterface
   interface ParameterlessRpcMethodFunction<Result> {
      Result apply(MinecraftApi var1, ClientInfo var2);
   }

   @FunctionalInterface
   interface RpcMethodFunction<Params, Result> {
      Result apply(MinecraftApi var1, Params var2, ClientInfo var3);
   }
}
