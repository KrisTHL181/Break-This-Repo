package net.minecraft.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import net.minecraft.core.IdMap;
import org.jspecify.annotations.Nullable;

public class CrudeIncrementalIntIdentityHashBiMap<K> implements IdMap<K> {
   private static final int NOT_FOUND = -1;
   private static final Object EMPTY_SLOT = null;
   private static final float LOADFACTOR = 0.8F;
   private @Nullable K[] keys;
   private int[] values;
   private @Nullable K[] byId;
   private int nextId;
   private int size;

   private CrudeIncrementalIntIdentityHashBiMap(final int capacity) {
      this.keys = (K[])(new Object[capacity]);
      this.values = new int[capacity];
      this.byId = (K[])(new Object[capacity]);
   }

   private CrudeIncrementalIntIdentityHashBiMap(final K[] keys, final int[] values, final K[] byId, final int nextId, final int size) {
      this.keys = keys;
      this.values = values;
      this.byId = byId;
      this.nextId = nextId;
      this.size = size;
   }

   public static <A> CrudeIncrementalIntIdentityHashBiMap<A> create(final int initialCapacity) {
      return new CrudeIncrementalIntIdentityHashBiMap((int)(initialCapacity / 0.8F));
   }

   @Override
   public int getId(final @Nullable K thing) {
      return this.getValue(this.indexOf(thing, this.hash(thing)));
   }

   @Override
   public @Nullable K byId(final int id) {
      return id >= 0 && id < this.byId.length ? this.byId[id] : null;
   }

   private int getValue(final int index) {
      return index == -1 ? -1 : this.values[index];
   }

   public boolean contains(final K key) {
      return this.getId(key) != -1;
   }

   public boolean contains(final int id) {
      return this.byId(id) != null;
   }

   public int add(final K key) {
      int value = this.nextId();
      this.addMapping(key, value);
      return value;
   }

   private int nextId() {
      while (this.nextId < this.byId.length && this.byId[this.nextId] != null) {
         this.nextId++;
      }

      return this.nextId;
   }

   private void grow(final int newSize) {
      K[] oldKeys = this.keys;
      int[] oldValues = this.values;
      CrudeIncrementalIntIdentityHashBiMap<K> resized = new CrudeIncrementalIntIdentityHashBiMap<>(newSize);

      for (int i = 0; i < oldKeys.length; i++) {
         if (oldKeys[i] != null) {
            resized.addMapping(oldKeys[i], oldValues[i]);
         }
      }

      this.keys = resized.keys;
      this.values = resized.values;
      this.byId = resized.byId;
      this.nextId = resized.nextId;
      this.size = resized.size;
   }

   public void addMapping(final K key, final int id) {
      int minSize = Math.max(id, this.size + 1);
      if (minSize >= this.keys.length * 0.8F) {
         int newSize = this.keys.length << 1;

         while (newSize < id) {
            newSize <<= 1;
         }

         this.grow(newSize);
      }

      int index = this.findEmpty(this.hash(key));
      this.keys[index] = key;
      this.values[index] = id;
      this.byId[id] = key;
      this.size++;
      if (id == this.nextId) {
         this.nextId++;
      }
   }

   private int hash(final @Nullable K key) {
      return (Mth.murmurHash3Mixer(System.identityHashCode(key)) & 2147483647) % this.keys.length;
   }

   private int indexOf(final @Nullable K key, final int startFrom) {
      for (int i = startFrom; i < this.keys.length; i++) {
         if (this.keys[i] == key) {
            return i;
         }

         if (this.keys[i] == EMPTY_SLOT) {
            return -1;
         }
      }

      for (int i = 0; i < startFrom; i++) {
         if (this.keys[i] == key) {
            return i;
         }

         if (this.keys[i] == EMPTY_SLOT) {
            return -1;
         }
      }

      return -1;
   }

   private int findEmpty(final int startFrom) {
      for (int i = startFrom; i < this.keys.length; i++) {
         if (this.keys[i] == EMPTY_SLOT) {
            return i;
         }
      }

      for (int i = 0; i < startFrom; i++) {
         if (this.keys[i] == EMPTY_SLOT) {
            return i;
         }
      }

      throw new RuntimeException("Overflowed :(");
   }

   @Override
   public Iterator<K> iterator() {
      return Iterators.filter(Iterators.forArray(this.byId), Predicates.notNull());
   }

   public void clear() {
      Arrays.fill(this.keys, null);
      Arrays.fill(this.byId, null);
      this.nextId = 0;
      this.size = 0;
   }

   @Override
   public int size() {
      return this.size;
   }

   public CrudeIncrementalIntIdentityHashBiMap<K> copy() {
      return new CrudeIncrementalIntIdentityHashBiMap<>(
         (K[])((Object[])this.keys.clone()), (int[])this.values.clone(), (K[])((Object[])this.byId.clone()), this.nextId, this.size
      );
   }
}
