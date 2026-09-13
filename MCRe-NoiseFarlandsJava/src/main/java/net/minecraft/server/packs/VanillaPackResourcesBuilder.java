package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FileSystemUtil;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Consumer<VanillaPackResourcesBuilder> developmentConfig = builder -> {};
    private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
        synchronized (VanillaPackResources.class) {
            Builder<PackType, Path> result = ImmutableMap.builder();

            for (PackType type : PackType.values()) {
                String probeName = "/" + type.getDirectory() + "/.mcassetsroot";
                URL probeUrl = VanillaPackResources.class.getResource(probeName);
                if (probeUrl == null) {
                    LOGGER.error("File {} does not exist in classpath", probeName);
                } else {
                    try {
                        URI probeUri = probeUrl.toURI();
                        String scheme = probeUri.getScheme();
                        if (!"jar".equals(scheme) && !"file".equals(scheme)) {
                            LOGGER.warn("Assets URL '{}' uses unexpected schema", probeUri);
                        }

                        Path probePath = FileSystemUtil.safeGetPath(probeUri);
                        result.put(type, probePath.getParent());
                    } catch (Exception e) {
                        LOGGER.error("Couldn't resolve path to vanilla assets", e);
                    }
                }
            }

            return result.build();
        }
    });
    private final Set<Path> rootPaths = new LinkedHashSet<>();
    private final Map<PackType, Set<Path>> pathsForType = new EnumMap<>(PackType.class);
    private ResourceMetadata metadata = ResourceMetadata.EMPTY;
    private final Set<String> namespaces = new HashSet<>();

    private boolean validateDirPath(final Path path) {
        if (!Files.exists(path)) {
            return false;
        } else if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path " + path.toAbsolutePath() + " is not directory");
        } else {
            return true;
        }
    }

    private void pushRootPath(final Path path) {
        if (this.validateDirPath(path)) {
            this.rootPaths.add(path);
        }
    }

    private void pushPathForType(final PackType packType, final Path path) {
        if (this.validateDirPath(path)) {
            this.pathsForType.computeIfAbsent(packType, k -> new LinkedHashSet<>()).add(path);
        }
    }

    public VanillaPackResourcesBuilder pushJarResources() {
        ROOT_DIR_BY_TYPE.forEach((packType, path) -> {
            this.pushRootPath(path.getParent());
            this.pushPathForType(packType, path);
        });
        return this;
    }

    public VanillaPackResourcesBuilder pushClasspathResources(final PackType packType, final Class<?> source) {
        Enumeration<URL> resources = null;

        try {
            resources = source.getClassLoader().getResources(packType.getDirectory() + "/");
        } catch (IOException var8) {
        }

        while (resources != null && resources.hasMoreElements()) {
            URL url = resources.nextElement();

            try {
                URI uri = url.toURI();
                if ("file".equals(uri.getScheme())) {
                    Path assetsPath = Paths.get(uri);
                    this.pushRootPath(assetsPath.getParent());
                    this.pushPathForType(packType, assetsPath);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to extract path from {}", url, e);
            }
        }

        return this;
    }

    public VanillaPackResourcesBuilder applyDevelopmentConfig() {
        developmentConfig.accept(this);
        return this;
    }

    public VanillaPackResourcesBuilder pushUniversalPath(final Path path) {
        this.pushRootPath(path);

        for (PackType packType : PackType.values()) {
            this.pushPathForType(packType, path.resolve(packType.getDirectory()));
        }

        return this;
    }

    public VanillaPackResourcesBuilder pushAssetPath(final PackType packType, final Path path) {
        this.pushRootPath(path);
        this.pushPathForType(packType, path);
        return this;
    }

    public VanillaPackResourcesBuilder setMetadata(final ResourceMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public VanillaPackResourcesBuilder exposeNamespace(final String... namespaces) {
        this.namespaces.addAll(Arrays.asList(namespaces));
        return this;
    }

    public VanillaPackResources build(final PackLocationInfo location) {
        return new VanillaPackResources(
            location,
            this.metadata,
            Set.copyOf(this.namespaces),
            copyAndReverse(this.rootPaths),
            Util.makeEnumMap(PackType.class, packType -> copyAndReverse(this.pathsForType.getOrDefault(packType, Set.of())))
        );
    }

    private static List<Path> copyAndReverse(final Collection<Path> input) {
        List<Path> paths = new ArrayList<>(input);
        Collections.reverse(paths);
        return List.copyOf(paths);
    }
}