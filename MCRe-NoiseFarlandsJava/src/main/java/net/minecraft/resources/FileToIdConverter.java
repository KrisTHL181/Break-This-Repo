package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public record FileToIdConverter(String prefix, String extension) {
    public static FileToIdConverter json(final String prefix) {
        return new FileToIdConverter(prefix, ".json");
    }

    public static FileToIdConverter registry(final ResourceKey<? extends Registry<?>> registry) {
        return json(Registries.elementsDirPath(registry));
    }

    public Identifier idToFile(final Identifier id) {
        return id.withPath(this.prefix + "/" + id.getPath() + this.extension);
    }

    public Identifier fileToId(final Identifier file) {
        String path = file.getPath();
        return file.withPath(path.substring(this.prefix.length() + 1, path.length() - this.extension.length()));
    }

    public boolean extensionMatches(final Identifier id) {
        return id.getPath().endsWith(this.extension);
    }

    public Map<Identifier, Resource> listMatchingResources(final ResourceManager manager) {
        return manager.listResources(this.prefix, this::extensionMatches);
    }

    public Map<Identifier, List<Resource>> listMatchingResourceStacks(final ResourceManager manager) {
        return manager.listResourceStacks(this.prefix, this::extensionMatches);
    }
}