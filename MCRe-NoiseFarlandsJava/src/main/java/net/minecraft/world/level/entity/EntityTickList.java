package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class EntityTickList {
    private Int2ObjectMap<Entity> active = new Int2ObjectLinkedOpenHashMap<>();
    private Int2ObjectMap<Entity> passive = new Int2ObjectLinkedOpenHashMap<>();
    private @Nullable Int2ObjectMap<Entity> iterated;

    private void ensureActiveIsNotIterated() {
        if (this.iterated == this.active) {
            this.passive.clear();

            for (Entry<Entity> entry : Int2ObjectMaps.fastIterable(this.active)) {
                this.passive.put(entry.getIntKey(), entry.getValue());
            }

            Int2ObjectMap<Entity> tmp = this.active;
            this.active = this.passive;
            this.passive = tmp;
        }
    }

    public void add(final Entity entity) {
        this.ensureActiveIsNotIterated();
        this.active.put(entity.getId(), entity);
    }

    public void remove(final Entity entity) {
        this.ensureActiveIsNotIterated();
        this.active.remove(entity.getId());
    }

    public boolean contains(final Entity entity) {
        return this.active.containsKey(entity.getId());
    }

    public void forEach(final Consumer<Entity> output) {
        if (this.iterated != null) {
            throw new UnsupportedOperationException("Only one concurrent iteration supported");
        }

        this.iterated = this.active;

        try {
            for (Entity entity : this.active.values()) {
                output.accept(entity);
            }
        } finally {
            this.iterated = null;
        }
    }
}