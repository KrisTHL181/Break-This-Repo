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

   JsonElement apply(MinecraftApi minecraftApi, @Nullable JsonElement paramsJson, ClientInfo clientInfo);

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(final IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> function) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(function);
   }

   static <Params, Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> method(final IncomingRpcMethod.RpcMethodFunction<Params, Result> function) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(function);
   }

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(final Function<MinecraftApi, Result> supplier) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(supplier);
   }

   record Attributes(boolean runOnMainThread, boolean discoverable, boolean allowPreServerInit) {
   }

   class IncomingRpcMethodBuilder<Params, Result> {
      private String description = "";
      private @Nullable ParamInfo<Params> paramInfo;
      private @Nullable ResultInfo<Result> resultInfo;
      private boolean discoverable = true;
      private boolean runOnMainThread = true;
      private IncomingRpcMethod.@Nullable ParameterlessRpcMethodFunction<Result> parameterlessFunction;
      private IncomingRpcMethod.@Nullable RpcMethodFunction<Params, Result> parameterFunction;
      private boolean allowPreServerInit = false;

      public IncomingRpcMethodBuilder(final IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> function) {
         this.parameterlessFunction = function;
      }

      public IncomingRpcMethodBuilder(final IncomingRpcMethod.RpcMethodFunction<Params, Result> function) {
         this.parameterFunction = function;
      }

      public IncomingRpcMethodBuilder(final Function<MinecraftApi, Result> supplier) {
         this.parameterlessFunction = (apiService, var2) -> supplier.apply(apiService);
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> description(final String description) {
         this.description = description;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> response(final String resultName, final Schema<Result> resultSchema) {
         this.resultInfo = new ResultInfo<>(resultName, resultSchema.info());
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> param(final String paramName, final Schema<Params> paramSchema) {
         this.paramInfo = new ParamInfo<>(paramName, paramSchema.info());
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

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> allowPreServerInit() {
         this.allowPreServerInit = true;
         return this;
      }

      public IncomingRpcMethod<Params, Result> build() {
         if (this.resultInfo == null) {
            throw new IllegalStateException("No response defined");
         }

         IncomingRpcMethod.Attributes attributes = new IncomingRpcMethod.Attributes(this.runOnMainThread, this.discoverable, this.allowPreServerInit);
         MethodInfo<Params, Result> methodInfo = new MethodInfo<>(this.description, this.paramInfo, this.resultInfo);
         if (this.parameterlessFunction != null) {
            return new IncomingRpcMethod.ParameterlessMethod<>(methodInfo, attributes, this.parameterlessFunction);
         }

         if (this.parameterFunction != null) {
            if (this.paramInfo == null) {
               throw new IllegalStateException("No param schema defined");
            } else {
               return new IncomingRpcMethod.Method<>(methodInfo, attributes, this.parameterFunction);
            }
         } else {
            throw new IllegalStateException("No method defined");
         }
      }

      public IncomingRpcMethod<?, ?> register(final Registry<IncomingRpcMethod<?, ?>> methodRegistry, final String key) {
         return this.register(methodRegistry, Identifier.withDefaultNamespace(key));
      }

      private IncomingRpcMethod<?, ?> register(final Registry<IncomingRpcMethod<?, ?>> methodRegistry, final Identifier id) {
         return Registry.register(methodRegistry, id, this.build());
      }
   }

   record Method<Params, Result>(
      MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.RpcMethodFunction<Params, Result> function
   ) implements IncomingRpcMethod<Params, Result> {
      @Override
      public JsonElement apply(final MinecraftApi minecraftApi, final @Nullable JsonElement paramsJson, final ClientInfo clientInfo) {
         if (paramsJson != null && (paramsJson.isJsonArray() || paramsJson.isJsonObject())) {
            if (this.info.params().isEmpty()) {
               throw new IllegalArgumentException("Method defined as having parameters without describing them");
            }

            JsonElement paramsJsonElement;
            if (paramsJson.isJsonObject()) {
               String parameterName = this.info.params().get().name();
               JsonElement jsonElement = paramsJson.getAsJsonObject().get(parameterName);
               if (jsonElement == null) {
                  throw new InvalidParameterJsonRpcException(
                     String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", parameterName)
                  );
               }

               paramsJsonElement = jsonElement;
            } else {
               JsonArray jsonArray = paramsJson.getAsJsonArray();
               if (jsonArray.isEmpty() || jsonArray.size() > 1) {
                  throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
               }

               paramsJsonElement = jsonArray.get(0);
            }

            Params params = (Params)this.info
               .params()
               .get()
               .schema()
               .codec()
               .parse(JsonOps.INSTANCE, paramsJsonElement)
               .getOrThrow(InvalidParameterJsonRpcException::new);
            Result result = this.function.apply(minecraftApi, params, clientInfo);
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
      public JsonElement apply(final MinecraftApi minecraftApi, final @Nullable JsonElement paramsJson, final ClientInfo clientInfo) {
         if (paramsJson == null || paramsJson.isJsonArray() && paramsJson.getAsJsonArray().isEmpty()) {
            if (this.info.params().isPresent()) {
               throw new IllegalArgumentException("Parameterless method unexpectedly has parameter description");
            } else {
               Result result = this.supplier.apply(minecraftApi, clientInfo);
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
      Result apply(MinecraftApi api, ClientInfo clientInfo);
   }

   @FunctionalInterface
   interface RpcMethodFunction<Params, Result> {
      Result apply(MinecraftApi api, Params params, ClientInfo clientInfo);
   }
}
