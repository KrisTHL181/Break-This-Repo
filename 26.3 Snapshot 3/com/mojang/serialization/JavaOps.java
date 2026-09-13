package com.mojang.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public class JavaOps implements DynamicOps<Object> {
   public static final JavaOps INSTANCE = new JavaOps();

   private JavaOps() {
   }

   @Override
   public Object empty() {
      return null;
   }

   @Override
   public Object emptyMap() {
      return Map.of();
   }

   @Override
   public Object emptyList() {
      return List.of();
   }

   @Override
   public <U> U convertTo(DynamicOps<U> outOps, Object input) {
      if (input == null) {
         return outOps.empty();
      } else if (input instanceof Map) {
         return this.convertMap(outOps, input);
      } else if (input instanceof ByteList value) {
         return outOps.createByteList(ByteBuffer.wrap(value.toByteArray()));
      } else if (input instanceof IntList value) {
         return outOps.createIntList(value.intStream());
      } else if (input instanceof LongList value) {
         return outOps.createLongList(value.longStream());
      } else if (input instanceof List) {
         return this.convertList(outOps, input);
      } else if (input instanceof String value) {
         return outOps.createString(value);
      } else if (input instanceof Boolean value) {
         return outOps.createBoolean(value);
      } else if (input instanceof Byte value) {
         return outOps.createByte(value);
      } else if (input instanceof Short value) {
         return outOps.createShort(value);
      } else if (input instanceof Integer value) {
         return outOps.createInt(value);
      } else if (input instanceof Long value) {
         return outOps.createLong(value);
      } else if (input instanceof Float value) {
         return outOps.createFloat(value);
      } else if (input instanceof Double value) {
         return outOps.createDouble(value);
      } else if (input instanceof Number value) {
         return outOps.createNumeric(value);
      } else {
         throw new IllegalStateException("Don't know how to convert " + input);
      }
   }

   @Override
   public DataResult<Number> getNumberValue(Object input) {
      return input instanceof Number value ? DataResult.success(value) : DataResult.error(() -> "Not a number: " + input);
   }

   @Override
   public Object createNumeric(Number value) {
      return value;
   }

   @Override
   public Object createByte(byte value) {
      return value;
   }

   @Override
   public Object createShort(short value) {
      return value;
   }

   @Override
   public Object createInt(int value) {
      return value;
   }

   @Override
   public Object createLong(long value) {
      return value;
   }

   @Override
   public Object createFloat(float value) {
      return value;
   }

   @Override
   public Object createDouble(double value) {
      return value;
   }

   @Override
   public DataResult<Boolean> getBooleanValue(Object input) {
      return input instanceof Boolean value ? DataResult.success(value) : DataResult.error(() -> "Not a boolean: " + input);
   }

   @Override
   public Object createBoolean(boolean value) {
      return value;
   }

   @Override
   public DataResult<String> getStringValue(Object input) {
      return input instanceof String value ? DataResult.success(value) : DataResult.error(() -> "Not a string: " + input);
   }

   @Override
   public Object createString(String value) {
      return value;
   }

   @Override
   public DataResult<Object> mergeToList(Object input, Object value) {
      if (input == this.empty()) {
         return DataResult.success(List.of(value));
      } else if (input instanceof List<?> list) {
         return list.isEmpty() ? DataResult.success(List.of(value)) : DataResult.success(ImmutableList.builder().addAll(list).add(value).build());
      } else {
         return DataResult.error(() -> "Not a list: " + input);
      }
   }

   @Override
   public DataResult<Object> mergeToList(Object input, List<Object> values) {
      if (input == this.empty()) {
         return DataResult.success(values);
      }

      if (input instanceof List<?> list) {
         if (values.isEmpty()) {
            return DataResult.success(list);
         } else {
            return list.isEmpty() ? DataResult.success(values) : DataResult.success(ImmutableList.builder().addAll(list).addAll(values).build());
         }
      } else {
         return DataResult.error(() -> "Not a list: " + input);
      }
   }

   @Override
   public DataResult<Object> mergeToMap(Object input, Object key, Object value) {
      if (input == this.empty()) {
         return DataResult.success(Map.of(key, value));
      }

      if (input instanceof Map<?, ?> map) {
         if (map.isEmpty()) {
            return DataResult.success(Map.of(key, value));
         }

         Builder<Object, Object> result = ImmutableMap.builderWithExpectedSize(map.size() + 1);
         result.putAll(map);
         result.put(key, value);
         return DataResult.success(result.buildKeepingLast());
      } else {
         return DataResult.error(() -> "Not a map: " + input);
      }
   }

   @Override
   public DataResult<Object> mergeToMap(Object input, Map<Object, Object> values) {
      if (input == this.empty()) {
         return DataResult.success(values);
      }

      if (input instanceof Map<?, ?> map) {
         if (values.isEmpty()) {
            return DataResult.success(map);
         }

         if (map.isEmpty()) {
            return DataResult.success(values);
         }

         Builder<Object, Object> result = ImmutableMap.builderWithExpectedSize(map.size() + values.size());
         result.putAll(map);
         result.putAll(values);
         return DataResult.success(result.buildKeepingLast());
      } else {
         return DataResult.error(() -> "Not a map: " + input);
      }
   }

   private static Map<Object, Object> mapLikeToMap(MapLike<Object> values) {
      return values.entries().collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
   }

   @Override
   public DataResult<Object> mergeToMap(Object input, MapLike<Object> values) {
      if (input == this.empty()) {
         return DataResult.success(mapLikeToMap(values));
      }

      if (input instanceof Map<?, ?> map) {
         if (map.isEmpty()) {
            return DataResult.success(mapLikeToMap(values));
         }

         Iterator<Pair<Object, Object>> valuesIterator = values.entries().iterator();
         if (!valuesIterator.hasNext()) {
            return DataResult.success(map);
         }

         Builder<Object, Object> result = ImmutableMap.builderWithExpectedSize(map.size());
         result.putAll(map);
         valuesIterator.forEachRemaining(e -> result.put(e.getFirst(), e.getSecond()));
         return DataResult.success(result.buildKeepingLast());
      } else {
         return DataResult.error(() -> "Not a map: " + input);
      }
   }

   private static Stream<Pair<Object, Object>> getMapEntries(Map<?, ?> input) {
      return input.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
   }

   @Override
   public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
      return input instanceof Map<?, ?> map ? DataResult.success(getMapEntries(map)) : DataResult.error(() -> "Not a map: " + input);
   }

   @Override
   public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(Object input) {
      return input instanceof Map<?, ?> map ? DataResult.success(map::forEach) : DataResult.error(() -> "Not a map: " + input);
   }

   @Override
   public Object createMap(Stream<Pair<Object, Object>> map) {
      return map.collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
   }

   @Override
   public DataResult<MapLike<Object>> getMap(Object input) {
      return input instanceof Map<?, ?> map ? DataResult.success(new MapLike<Object>() {
         @Nullable
         @Override
         public Object get(Object key) {
            return map.get(key);
         }

         @Nullable
         @Override
         public Object get(String key) {
            return map.get(key);
         }

         @Override
         public Stream<Pair<Object, Object>> entries() {
            return JavaOps.getMapEntries(map);
         }

         @Override
         public String toString() {
            return "MapLike[" + map + "]";
         }
      }) : DataResult.error(() -> "Not a map: " + input);
   }

   @Override
   public Object createMap(Map<Object, Object> map) {
      return map;
   }

   @Override
   public DataResult<Stream<Object>> getStream(Object input) {
      return input instanceof List<?> list ? DataResult.success(list.stream().map(o -> o)) : DataResult.error(() -> "Not an list: " + input);
   }

   @Override
   public DataResult<Consumer<Consumer<Object>>> getList(Object input) {
      return input instanceof List<?> list ? DataResult.success(list::forEach) : DataResult.error(() -> "Not an list: " + input);
   }

   @Override
   public Object createList(Stream<Object> input) {
      return input.toList();
   }

   @Override
   public DataResult<ByteBuffer> getByteBuffer(Object input) {
      return input instanceof ByteList value ? DataResult.success(ByteBuffer.wrap(value.toByteArray())) : DataResult.error(() -> "Not a byte list: " + input);
   }

   @Override
   public Object createByteList(ByteBuffer input) {
      ByteBuffer wholeBuffer = input.duplicate().clear();
      ByteArrayList result = new ByteArrayList();
      result.size(wholeBuffer.capacity());
      wholeBuffer.get(0, result.elements(), 0, result.size());
      return result;
   }

   @Override
   public DataResult<IntStream> getIntStream(Object input) {
      return input instanceof IntList value ? DataResult.success(value.intStream()) : DataResult.error(() -> "Not an int list: " + input);
   }

   @Override
   public Object createIntList(IntStream input) {
      return IntArrayList.toList(input);
   }

   @Override
   public DataResult<LongStream> getLongStream(Object input) {
      return input instanceof LongList value ? DataResult.success(value.longStream()) : DataResult.error(() -> "Not a long list: " + input);
   }

   @Override
   public Object createLongList(LongStream input) {
      return LongArrayList.toList(input);
   }

   @Override
   public Object remove(Object input, String key) {
      if (input instanceof Map<?, ?> map) {
         Map<Object, Object> result = new LinkedHashMap<>((Map<? extends Object, ? extends Object>)map);
         result.remove(key);
         return Map.copyOf(result);
      } else {
         return input;
      }
   }

   @Override
   public RecordBuilder<Object> mapBuilder() {
      return new JavaOps.FixedMapBuilder<>(this);
   }

   @Override
   public String toString() {
      return "Java";
   }

   private static final class FixedMapBuilder<T> extends RecordBuilder.AbstractUniversalBuilder<T, Builder<T, T>> {
      public FixedMapBuilder(DynamicOps<T> ops) {
         super(ops);
      }

      protected Builder<T, T> initBuilder() {
         return ImmutableMap.builder();
      }

      protected Builder<T, T> append(T key, T value, Builder<T, T> builder) {
         return builder.put(key, value);
      }

      protected DataResult<T> build(Builder<T, T> builder, T prefix) {
         ImmutableMap<T, T> result = builder.buildKeepingLast();
         return this.ops().mergeToMap(prefix, result);
      }
   }
}
