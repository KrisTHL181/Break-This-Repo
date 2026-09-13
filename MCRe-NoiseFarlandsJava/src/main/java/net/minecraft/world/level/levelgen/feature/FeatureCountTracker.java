package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class FeatureCountTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LoadingCache<ServerLevel, FeatureCountTracker.LevelData> data = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterAccess(5L, TimeUnit.MINUTES)
        .build(new CacheLoader<ServerLevel, FeatureCountTracker.LevelData>() {
            public FeatureCountTracker.LevelData load(final ServerLevel level) {
                return new FeatureCountTracker.LevelData(Object2IntMaps.synchronize(new Object2IntOpenHashMap<>()), new MutableInt(0));
            }
        });

    public static void chunkDecorated(final ServerLevel level) {
        try {
            data.get(level).chunksWithFeatures().increment();
        } catch (Exception e) {
            LOGGER.error("Failed to increment chunk count", e);
        }
    }

    public static void featurePlaced(final ServerLevel level, final ConfiguredFeature<?, ?> feature, final Optional<PlacedFeature> topFeature) {
        try {
            data.get(level).featureData().computeInt(new FeatureCountTracker.FeatureData(feature, topFeature), (f, old) -> old == null ? 1 : old + 1);
        } catch (Exception e) {
            LOGGER.error("Failed to increment feature count", e);
        }
    }

    public static void clearCounts() {
        data.invalidateAll();
        LOGGER.debug("Cleared feature counts");
    }

    public static void logCounts() {
        LOGGER.debug("Logging feature counts:");
        data.asMap()
            .forEach(
                (level, featureCounts) -> {
                    String name = level.dimension().identifier().toString();
                    boolean running = level.getServer().isRunning();
                    Registry<PlacedFeature> featureRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
                    String prefix = (running ? "running" : "dead") + " " + name;
                    int chunks = featureCounts.chunksWithFeatures().intValue();
                    LOGGER.debug("{} total_chunks: {}", prefix, chunks);
                    featureCounts.featureData()
                        .forEach(
                            (data, count) -> LOGGER.debug(
                                "{} {} {} {} {} {}",
                                prefix,
                                String.format(Locale.ROOT, "%10d", count),
                                String.format(Locale.ROOT, "%10f", (double)count / chunks),
                                data.topFeature().flatMap(featureRegistry::getResourceKey).map(ResourceKey::identifier),
                                data.feature().feature(),
                                data.feature()
                            )
                        );
                }
            );
    }

    private record FeatureData(ConfiguredFeature<?, ?> feature, Optional<PlacedFeature> topFeature) {
    }

    private record LevelData(Object2IntMap<FeatureCountTracker.FeatureData> featureData, MutableInt chunksWithFeatures) {
    }
}