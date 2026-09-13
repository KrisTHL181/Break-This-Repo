package net.minecraft.server.jsonrpc.dataprovider;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.methods.DiscoveryService;

public class JsonRpcApiSchema implements DataProvider {
   private final Path path;

   public JsonRpcApiSchema(final PackOutput packOutput) {
      this.path = packOutput.getOutputFolder(PackOutput.Target.REPORTS).resolve("json-rpc-api-schema.json");
   }

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      DiscoveryService.DiscoverResponse discover = DiscoveryService.discover(Schema.getSchemaRegistry());
      return DataProvider.saveStable(
         cache, (JsonElement)DiscoveryService.DiscoverResponse.CODEC.codec().encodeStart(JsonOps.INSTANCE, discover).getOrThrow(), this.path
      );
   }

   @Override
   public String getName() {
      return "Json RPC API schema";
   }
}
