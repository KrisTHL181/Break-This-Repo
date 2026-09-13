package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public interface PackResources extends AutoCloseable {
    String METADATA_EXTENSION = ".mcmeta";
    String PACK_META = "pack.mcmeta";

    @Nullable IoSupplier<InputStream> getRootResource(String... path);

    @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location);

    void listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output);

    Set<String> getNamespaces(PackType type);

    <T> @Nullable T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException;

    PackLocationInfo location();

    default String packId() {
        return this.location().id();
    }

    default Optional<KnownPack> knownPackInfo() {
        return this.location().knownPackInfo();
    }

    @Override
    void close();

    @FunctionalInterface
    interface ResourceOutput extends BiConsumer<Identifier, IoSupplier<InputStream>> {
    }
}