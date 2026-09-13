package net.minecraft.core.registries;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ConcurrentHolderGetter<T> implements HolderGetter<T> {
    private final Object lock;
    private final HolderGetter<T> original;
    private final Map<ResourceKey<T>, Optional<Holder.Reference<T>>> elementCache = new ConcurrentHashMap<>();
    private final Map<TagKey<T>, Optional<HolderSet.Named<T>>> tagCache = new ConcurrentHashMap<>();

    public ConcurrentHolderGetter(final Object lock, final HolderGetter<T> original) {
        this.lock = lock;
        this.original = original;
    }

    @Override
    public Optional<Holder.Reference<T>> get(final ResourceKey<T> elementId) {
        return this.elementCache.computeIfAbsent(elementId, id -> {
            synchronized (this.lock) {
                return this.original.get((ResourceKey<T>)id);
            }
        });
    }

    @Override
    public Optional<HolderSet.Named<T>> get(final TagKey<T> tagId) {
        return this.tagCache.computeIfAbsent(tagId, id -> {
            synchronized (this.lock) {
                return this.original.get((TagKey<T>)id);
            }
        });
    }
}