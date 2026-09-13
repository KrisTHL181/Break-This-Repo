package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.api.SchemaComponent;

public class DiscoveryService {
   public static DiscoveryService.DiscoverResponse discover(List<SchemaComponent<?>> p_429761_) {
      List<MethodInfo.Named<?, ?>> list = new ArrayList<>(BuiltInRegistries.INCOMING_RPC_METHOD.size() + BuiltInRegistries.OUTGOING_RPC_METHOD.size());
      BuiltInRegistries.INCOMING_RPC_METHOD.listElements().forEach(p_449132_ -> {
         if (p_449132_.value().attributes().discoverable()) {
            list.add(p_449132_.value().info().named(p_449132_.key().identifier()));
         }
      });
      BuiltInRegistries.OUTGOING_RPC_METHOD.listElements().forEach(p_449130_ -> {
         if (p_449130_.value().attributes().discoverable()) {
            list.add(p_449130_.value().info().named(p_449130_.key().identifier()));
         }
      });
      Map<String, Schema<?>> map = new HashMap<>();

      for (SchemaComponent<?> schemacomponent : p_429761_) {
         map.put(schemacomponent.name(), schemacomponent.schema().info());
      }

      DiscoveryService.DiscoverInfo discoveryservice$discoverinfo = new DiscoveryService.DiscoverInfo("Minecraft Server JSON-RPC", "2.0.0");
      return new DiscoveryService.DiscoverResponse("1.3.2", discoveryservice$discoverinfo, list, new DiscoveryService.DiscoverComponents(map));
   }

   public record DiscoverComponents(Map<String, Schema<?>> schemas) {
      public static final MapCodec<DiscoveryService.DiscoverComponents> CODEC = typedSchema();

      private static MapCodec<DiscoveryService.DiscoverComponents> typedSchema() {
         return RecordCodecBuilder.mapCodec(
            p_427036_ -> p_427036_.group(
                  Codec.unboundedMap(Codec.STRING, Schema.CODEC).fieldOf("schemas").forGetter(DiscoveryService.DiscoverComponents::schemas)
               )
               .apply(p_427036_, DiscoveryService.DiscoverComponents::new)
         );
      }
   }

   public record DiscoverInfo(String title, String version) {
      public static final MapCodec<DiscoveryService.DiscoverInfo> CODEC = RecordCodecBuilder.mapCodec(
         p_427195_ -> p_427195_.group(
               Codec.STRING.fieldOf("title").forGetter(DiscoveryService.DiscoverInfo::title),
               Codec.STRING.fieldOf("version").forGetter(DiscoveryService.DiscoverInfo::version)
            )
            .apply(p_427195_, DiscoveryService.DiscoverInfo::new)
      );
   }

   public record DiscoverResponse(
      String jsonRpcProtocolVersion,
      DiscoveryService.DiscoverInfo discoverInfo,
      List<MethodInfo.Named<?, ?>> methods,
      DiscoveryService.DiscoverComponents components
   ) {
      public static final MapCodec<DiscoveryService.DiscoverResponse> CODEC = RecordCodecBuilder.mapCodec(
         p_426847_ -> p_426847_.group(
               Codec.STRING.fieldOf("openrpc").forGetter(DiscoveryService.DiscoverResponse::jsonRpcProtocolVersion),
               DiscoveryService.DiscoverInfo.CODEC.codec().fieldOf("info").forGetter(DiscoveryService.DiscoverResponse::discoverInfo),
               Codec.list(MethodInfo.Named.CODEC).fieldOf("methods").forGetter(DiscoveryService.DiscoverResponse::methods),
               DiscoveryService.DiscoverComponents.CODEC.codec().fieldOf("components").forGetter(DiscoveryService.DiscoverResponse::components)
            )
            .apply(p_426847_, DiscoveryService.DiscoverResponse::new)
      );
   }
}
