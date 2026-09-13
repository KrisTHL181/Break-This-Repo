package net.minecraft.world.level.entity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.AbortableIterationConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EntityLookup<T extends EntityAccess> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Int2ObjectMap<T> byId = new Int2ObjectLinkedOpenHashMap<>();
    private final Map<UUID, T> byUuid = Maps.newHashMap();

    public <U extends T> void getEntities(final EntityTypeTest<T, U> type, final AbortableIterationConsumer<U> consumer) {
        for (T entity : this.byId.values()) {
            U maybeEntity = (U)type.tryCast(entity);
            if (maybeEntity != null && consumer.accept(maybeEntity).shouldAbort()) {
                return;
            }
        }
    }

    public Iterable<T> getAllEntities() {
        return Iterables.unmodifiableIterable(this.byId.values());
    }

    public void add(final T entity) {
        UUID uuid = entity.getUUID();
        if (this.byUuid.containsKey(uuid)) {
            LOGGER.warn("Duplicate entity UUID {}: {}", uuid, entity);
        } else {
            this.byUuid.put(uuid, entity);
            this.byId.put(entity.getId(), entity);
        }
    }

    public void remove(final T entity) {
        this.byUuid.remove(entity.getUUID());
        this.byId.remove(entity.getId());
    }

    public @Nullable T getEntity(final int id) {
        return this.byId.get(id);
    }

    public @Nullable T getEntity(final UUID id) {
        return this.byUuid.get(id);
    }

    public int count() {
        return this.byUuid.size();
    }
}