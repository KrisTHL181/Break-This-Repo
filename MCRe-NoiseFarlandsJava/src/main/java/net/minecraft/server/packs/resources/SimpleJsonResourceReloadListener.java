package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DynamicOps<JsonElement> ops;
    private final Codec<T> codec;
    private final FileToIdConverter lister;

    protected SimpleJsonResourceReloadListener(
        final HolderLookup.Provider registries, final Codec<T> codec, final ResourceKey<? extends Registry<T>> registryKey
    ) {
        this(registries.createSerializationContext(JsonOps.INSTANCE), codec, FileToIdConverter.registry(registryKey));
    }

    protected SimpleJsonResourceReloadListener(final Codec<T> codec, final FileToIdConverter lister) {
        this(JsonOps.INSTANCE, codec, lister);
    }

    private SimpleJsonResourceReloadListener(final DynamicOps<JsonElement> ops, final Codec<T> codec, final FileToIdConverter lister) {
        this.ops = ops;
        this.codec = codec;
        this.lister = lister;
    }

    protected Map<Identifier, T> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
        Map<Identifier, T> result = new HashMap<>();
        scanDirectory(manager, this.lister, this.ops, this.codec, result);
        return result;
    }

    public static <T> void scanDirectory(
        final ResourceManager manager,
        final ResourceKey<? extends Registry<T>> registryKey,
        final DynamicOps<JsonElement> ops,
        final Codec<T> codec,
        final Map<Identifier, T> result
    ) {
        scanDirectory(manager, FileToIdConverter.registry(registryKey), ops, codec, result);
    }

    public static <T> void scanDirectory(
        final ResourceManager manager, final FileToIdConverter lister, final DynamicOps<JsonElement> ops, final Codec<T> codec, final Map<Identifier, T> result
    ) {
        for (Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                codec.parse(ops, StrictJsonParser.parse(reader)).ifSuccess(parsed -> {
                    if (result.putIfAbsent(id, (T)parsed) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                    }
                }).ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", id, location, error));
            } catch (JsonParseException | IllegalArgumentException | IOException e) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, e);
            }
        }
    }
}