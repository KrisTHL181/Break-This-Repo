package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

public class ValueInputContextHelper {
   private final HolderLookup.Provider lookup;
   private final DynamicOps<Tag> ops;
   private final ValueInput.ValueInputList emptyChildList = new ValueInput.ValueInputList() {
      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Stream<ValueInput> stream() {
         return Stream.empty();
      }

      @Override
      public Iterator<ValueInput> iterator() {
         return Collections.emptyIterator();
      }
   };
   private final ValueInput.TypedInputList<Object> emptyTypedList = new ValueInput.TypedInputList<Object>() {
      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Stream<Object> stream() {
         return Stream.empty();
      }

      @Override
      public Iterator<Object> iterator() {
         return Collections.emptyIterator();
      }
   };
   private final ValueInput empty = new ValueInput() {
      @Override
      public <T> Optional<T> read(final String name, final Codec<T> codec) {
         return Optional.empty();
      }

      @Override
      public <T> Optional<T> read(final MapCodec<T> codec) {
         return Optional.empty();
      }

      @Override
      public Optional<ValueInput> child(final String name) {
         return Optional.empty();
      }

      @Override
      public ValueInput childOrEmpty(final String name) {
         return this;
      }

      @Override
      public Optional<ValueInput.ValueInputList> childrenList(final String name) {
         return Optional.empty();
      }

      @Override
      public ValueInput.ValueInputList childrenListOrEmpty(final String name) {
         return ValueInputContextHelper.this.emptyChildList;
      }

      @Override
      public <T> Optional<ValueInput.TypedInputList<T>> list(final String name, final Codec<T> codec) {
         return Optional.empty();
      }

      @Override
      public <T> ValueInput.TypedInputList<T> listOrEmpty(final String name, final Codec<T> codec) {
         return ValueInputContextHelper.this.emptyTypedList();
      }

      @Override
      public boolean getBooleanOr(final String name, final boolean defaultValue) {
         return defaultValue;
      }

      @Override
      public byte getByteOr(final String name, final byte defaultValue) {
         return defaultValue;
      }

      @Override
      public int getShortOr(final String name, final short defaultValue) {
         return defaultValue;
      }

      @Override
      public Optional<Integer> getInt(final String name) {
         return Optional.empty();
      }

      @Override
      public int getIntOr(final String name, final int defaultValue) {
         return defaultValue;
      }

      @Override
      public long getLongOr(final String name, final long defaultValue) {
         return defaultValue;
      }

      @Override
      public Optional<Long> getLong(final String name) {
         return Optional.empty();
      }

      @Override
      public float getFloatOr(final String name, final float defaultValue) {
         return defaultValue;
      }

      @Override
      public double getDoubleOr(final String name, final double defaultValue) {
         return defaultValue;
      }

      @Override
      public Optional<String> getString(final String name) {
         return Optional.empty();
      }

      @Override
      public String getStringOr(final String name, final String defaultValue) {
         return defaultValue;
      }

      @Override
      public HolderLookup.Provider lookup() {
         return ValueInputContextHelper.this.lookup;
      }

      @Override
      public Optional<int[]> getIntArray(final String name) {
         return Optional.empty();
      }
   };

   public ValueInputContextHelper(final HolderLookup.Provider lookup, final DynamicOps<Tag> ops) {
      this.lookup = lookup;
      this.ops = lookup.createSerializationContext(ops);
   }

   public DynamicOps<Tag> ops() {
      return this.ops;
   }

   public HolderLookup.Provider lookup() {
      return this.lookup;
   }

   public ValueInput empty() {
      return this.empty;
   }

   public ValueInput.ValueInputList emptyList() {
      return this.emptyChildList;
   }

   public <T> ValueInput.TypedInputList<T> emptyTypedList() {
      return (ValueInput.TypedInputList<T>)this.emptyTypedList;
   }
}
