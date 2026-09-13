package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.KnownPack;

public class RegistrySynchronization {
    private static final Set<ResourceKey<? extends Registry<?>>> NETWORKABLE_REGISTRIES = RegistryDataLoader.SYNCHRONIZED_REGISTRIES
        .stream()
        .map(RegistryDataLoader.RegistryData::key)
        .collect(Collectors.toUnmodifiableSet());

    public static void packRegistries(
        final DynamicOps<Tag> ops,
        final RegistryAccess registries,
        final Set<KnownPack> clientKnownPacks,
        final BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> output
    ) {
        RegistryDataLoader.SYNCHRONIZED_REGISTRIES
            .forEach(registryEntry -> packRegistry(ops, (RegistryDataLoader.RegistryData<?>)registryEntry, registries, clientKnownPacks, output));
    }

    private static <T> void packRegistry(
        final DynamicOps<Tag> ops,
        final RegistryDataLoader.RegistryData<T> registryData,
        final RegistryAccess registries,
        final Set<KnownPack> clientKnownPacks,
        final BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> output
    ) {
        registries.lookup(registryData.key())
            .ifPresent(
                registry -> {
                    List<RegistrySynchronization.PackedRegistryEntry> packedElements = new ArrayList<>(registry.size());
                    registry.listElements()
                        .forEach(
                            element -> {
                                boolean canSkipContents = registry.registrationInfo(element.key())
                                    .flatMap(RegistrationInfo::knownPackInfo)
                                    .filter(clientKnownPacks::contains)
                                    .isPresent();
                                Optional<Tag> contents;
                                if (canSkipContents) {
                                    contents = Optional.empty();
                                } else {
                                    Tag encodedElement = registryData.elementCodec()
                                        .encodeStart(ops, element.value())
                                        .getOrThrow(s -> new IllegalArgumentException("Failed to serialize " + element.key() + ": " + s));
                                    contents = Optional.of(encodedElement);
                                }

                                packedElements.add(new RegistrySynchronization.PackedRegistryEntry(element.key().identifier(), contents));
                            }
                        );
                    output.accept(registry.key(), packedElements);
                }
            );
    }

    private static Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries(final RegistryAccess access) {
        return access.registries().filter(e -> isNetworkable(e.key()));
    }

    public static Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries(final LayeredRegistryAccess<RegistryLayer> registries) {
        return ownedNetworkableRegistries(registries.getAccessFrom(RegistryLayer.WORLDGEN));
    }

    public static Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries(final LayeredRegistryAccess<RegistryLayer> registries) {
        Stream<RegistryAccess.RegistryEntry<?>> staticRegistries = registries.getLayer(RegistryLayer.STATIC).registries();
        Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries = networkedRegistries(registries);
        return Stream.concat(networkedRegistries, staticRegistries);
    }

    public static boolean isNetworkable(final ResourceKey<? extends Registry<?>> key) {
        return NETWORKABLE_REGISTRIES.contains(key);
    }

    public record PackedRegistryEntry(Identifier id, Optional<Tag> data) {
        public static final StreamCodec<ByteBuf, RegistrySynchronization.PackedRegistryEntry> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            RegistrySynchronization.PackedRegistryEntry::id,
            ByteBufCodecs.TAG.apply(ByteBufCodecs::optional),
            RegistrySynchronization.PackedRegistryEntry::data,
            RegistrySynchronization.PackedRegistryEntry::new
        );
    }
}