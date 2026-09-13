package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.Nullable;

public abstract class AbstractPackResources implements PackResources {
    private final PackLocationInfo location;
    private @Nullable ResourceMetadata metadata;

    protected AbstractPackResources(final PackLocationInfo location) {
        this.location = location;
    }

    @Override
    public <T> @Nullable T getMetadataSection(final MetadataSectionType<T> metadataSerializer) throws IOException {
        if (this.metadata == null) {
            this.metadata = loadMetadata(this);
        }

        return this.metadata.getSection(metadataSerializer).orElse(null);
    }

    public static ResourceMetadata loadMetadata(final PackResources packResources) throws IOException {
        IoSupplier<InputStream> metadata = packResources.getRootResource("pack.mcmeta");
        if (metadata == null) {
            return ResourceMetadata.EMPTY;
        }

        try (InputStream resource = metadata.get()) {
            return ResourceMetadata.fromJsonStream(resource);
        }
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }
}