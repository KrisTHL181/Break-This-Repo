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
    public static DiscoveryService.DiscoverResponse discover(final List<SchemaComponent<?>> schemaRegistry) {
        List<MethodInfo.Named<?, ?>> methods = new ArrayList<>(BuiltInRegistries.INCOMING_RPC_METHOD.size() + BuiltInRegistries.OUTGOING_RPC_METHOD.size());
        BuiltInRegistries.INCOMING_RPC_METHOD.listElements().forEach(e -> {
            if (e.value().attributes().discoverable()) {
                methods.add(e.value().info().named(e.key().identifier()));
            }
        });
        BuiltInRegistries.OUTGOING_RPC_METHOD.listElements().forEach(e -> {
            if (e.value().attributes().discoverable()) {
                methods.add(e.value().info().named(e.key().identifier()));
            }
        });
        Map<String, Schema<?>> schemas = new HashMap<>();

        for (SchemaComponent<?> component : schemaRegistry) {
            schemas.put(component.name(), component.schema().info());
        }

        DiscoveryService.DiscoverInfo discoverInfo = new DiscoveryService.DiscoverInfo("Minecraft Server JSON-RPC", "3.0.0");
        return new DiscoveryService.DiscoverResponse("1.3.2", discoverInfo, methods, new DiscoveryService.DiscoverComponents(schemas));
    }

    public record DiscoverComponents(Map<String, Schema<?>> schemas) {
        public static final MapCodec<DiscoveryService.DiscoverComponents> CODEC = typedSchema();

        private static MapCodec<DiscoveryService.DiscoverComponents> typedSchema() {
            return RecordCodecBuilder.mapCodec(
                i -> i.group(Codec.unboundedMap(Codec.STRING, (Codec<Schema<?>>)Schema.CODEC).fieldOf("schemas").forGetter(DiscoveryService.DiscoverComponents::schemas))
                    .apply(i, DiscoveryService.DiscoverComponents::new)
            );
        }
    }

    public record DiscoverInfo(String title, String version) {
        public static final MapCodec<DiscoveryService.DiscoverInfo> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.STRING.fieldOf("title").forGetter(DiscoveryService.DiscoverInfo::title),
                    Codec.STRING.fieldOf("version").forGetter(DiscoveryService.DiscoverInfo::version)
                )
                .apply(i, DiscoveryService.DiscoverInfo::new)
        );
    }

    public record DiscoverResponse(
        String jsonRpcProtocolVersion,
        DiscoveryService.DiscoverInfo discoverInfo,
        List<MethodInfo.Named<?, ?>> methods,
        DiscoveryService.DiscoverComponents components
    ) {
        public static final MapCodec<DiscoveryService.DiscoverResponse> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.STRING.fieldOf("openrpc").forGetter(DiscoveryService.DiscoverResponse::jsonRpcProtocolVersion),
                    DiscoveryService.DiscoverInfo.CODEC.codec().fieldOf("info").forGetter(DiscoveryService.DiscoverResponse::discoverInfo),
                    Codec.list(MethodInfo.Named.CODEC).fieldOf("methods").forGetter(DiscoveryService.DiscoverResponse::methods),
                    DiscoveryService.DiscoverComponents.CODEC.codec().fieldOf("components").forGetter(DiscoveryService.DiscoverResponse::components)
                )
                .apply(i, DiscoveryService.DiscoverResponse::new)
        );
    }
}
