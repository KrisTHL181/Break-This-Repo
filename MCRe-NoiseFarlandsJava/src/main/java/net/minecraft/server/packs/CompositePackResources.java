package net.minecraft.server.packs;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public class CompositePackResources implements PackResources {
    private final PackResources primaryPackResources;
    private final List<PackResources> packResourcesStack;

    public CompositePackResources(final PackResources primaryPackResources, final List<PackResources> overlayPackResources) {
        this.primaryPackResources = primaryPackResources;
        List<PackResources> stack = new ArrayList<>(overlayPackResources.size() + 1);
        stack.addAll(Lists.reverse(overlayPackResources));
        stack.add(primaryPackResources);
        this.packResourcesStack = List.copyOf(stack);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(final String... path) {
        return this.primaryPackResources.getRootResource(path);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(final PackType type, final Identifier location) {
        for (PackResources packResources : this.packResourcesStack) {
            IoSupplier<InputStream> resource = packResources.getResource(type, location);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    @Override
    public void listResources(final PackType type, final String namespace, final String directory, final PackResources.ResourceOutput output) {
        Map<Identifier, IoSupplier<InputStream>> result = new HashMap<>();

        for (PackResources packResources : this.packResourcesStack) {
            packResources.listResources(type, namespace, directory, result::putIfAbsent);
        }

        result.forEach(output);
    }

    @Override
    public Set<String> getNamespaces(final PackType type) {
        Set<String> result = new HashSet<>();

        for (PackResources overlayPackResource : this.packResourcesStack) {
            result.addAll(overlayPackResource.getNamespaces(type));
        }

        return result;
    }

    @Override
    public <T> @Nullable T getMetadataSection(final MetadataSectionType<T> metadataSerializer) throws IOException {
        return this.primaryPackResources.getMetadataSection(metadataSerializer);
    }

    @Override
    public PackLocationInfo location() {
        return this.primaryPackResources.location();
    }

    @Override
    public void close() {
        this.packResourcesStack.forEach(PackResources::close);
    }
}