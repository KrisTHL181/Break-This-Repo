package com.mojang.serialization;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

public class JsonOps implements DynamicOps<JsonElement> {
   public static final JsonOps INSTANCE = new JsonOps(false);
   public static final JsonOps COMPRESSED = new JsonOps(true);
   private final boolean compressed;

   protected JsonOps(boolean compressed) {
      this.compressed = compressed;
   }

   public JsonElement empty() {
      return JsonNull.INSTANCE;
   }

   public JsonElement emptyMap() {
      return new JsonObject();
   }

   public JsonElement emptyList() {
      return new JsonArray();
   }

   public <U> U convertTo(DynamicOps<U> outOps, JsonElement input) {
      if (input instanceof JsonObject) {
         return this.convertMap(outOps, input);
      }

      if (input instanceof JsonArray) {
         return this.convertList(outOps, input);
      }

      if (input instanceof JsonNull) {
         return outOps.empty();
      }

      JsonPrimitive primitive = input.getAsJsonPrimitive();
      if (primitive.isString()) {
         return outOps.createString(primitive.getAsString());
      }

      if (primitive.isBoolean()) {
         return outOps.createBoolean(primitive.getAsBoolean());
      }

      BigDecimal value = primitive.getAsBigDecimal();

      try {
         long l = value.longValueExact();
         if ((byte)l == l) {
            return outOps.createByte((byte)l);
         } else if ((short)l == l) {
            return outOps.createShort((short)l);
         } else {
            return (int)l == l ? outOps.createInt((int)l) : outOps.createLong(l);
         }
      } catch (ArithmeticException e) {
         double d = value.doubleValue();
         return (float)d == d ? outOps.createFloat((float)d) : outOps.createDouble(d);
      }
   }

   public DataResult<Number> getNumberValue(JsonElement input) {
      if (input instanceof JsonPrimitive) {
         if (input.getAsJsonPrimitive().isNumber()) {
            return DataResult.success(input.getAsNumber());
         }

         if (this.compressed && input.getAsJsonPrimitive().isString()) {
            try {
               return DataResult.success(Integer.parseInt(input.getAsString()));
            } catch (NumberFormatException e) {
               return DataResult.error(() -> "Not a number: " + e + " " + input);
            }
         }
      }

      return DataResult.error(() -> "Not a number: " + input);
   }

   public JsonElement createNumeric(Number i) {
      return new JsonPrimitive(i);
   }

   public DataResult<Boolean> getBooleanValue(JsonElement input) {
      return input instanceof JsonPrimitive && input.getAsJsonPrimitive().isBoolean()
         ? DataResult.success(input.getAsBoolean())
         : DataResult.error(() -> "Not a boolean: " + input);
   }

   public JsonElement createBoolean(boolean value) {
      return new JsonPrimitive(value);
   }

   public DataResult<String> getStringValue(JsonElement input) {
      return !(input instanceof JsonPrimitive) || !input.getAsJsonPrimitive().isString() && (!input.getAsJsonPrimitive().isNumber() || !this.compressed)
         ? DataResult.error(() -> "Not a string: " + input)
         : DataResult.success(input.getAsString());
   }

   public JsonElement createString(String value) {
      return new JsonPrimitive(value);
   }

   public DataResult<JsonElement> mergeToList(JsonElement list, JsonElement value) {
      if (!(list instanceof JsonArray) && list != this.empty()) {
         return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
      }

      JsonArray result = new JsonArray();
      if (list != this.empty()) {
         result.addAll(list.getAsJsonArray());
      }

      result.add(value);
      return DataResult.success(result);
   }

   public DataResult<JsonElement> mergeToList(JsonElement list, List<JsonElement> values) {
      if (!(list instanceof JsonArray) && list != this.empty()) {
         return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
      }

      if (values.isEmpty()) {
         return list == this.empty() ? DataResult.success(this.emptyList()) : DataResult.success(list);
      }

      JsonArray result = new JsonArray();
      if (list != this.empty()) {
         result.addAll(list.getAsJsonArray());
      }

      values.forEach(result::add);
      return DataResult.success(result);
   }

   public DataResult<JsonElement> mergeToMap(JsonElement map, JsonElement key, JsonElement value) {
      if (!(map instanceof JsonObject) && map != this.empty()) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
      }

      if (key instanceof JsonPrimitive && (key.getAsJsonPrimitive().isString() || this.compressed)) {
         JsonObject output = new JsonObject();
         if (map != this.empty()) {
            map.getAsJsonObject().entrySet().forEach(entry -> output.add((String)entry.getKey(), (JsonElement)entry.getValue()));
         }

         output.add(key.getAsString(), value);
         return DataResult.success(output);
      } else {
         return DataResult.error(() -> "key is not a string: " + key, map);
      }
   }

   public DataResult<JsonElement> mergeToMap(JsonElement map, MapLike<JsonElement> values) {
      if (!(map instanceof JsonObject) && map != this.empty()) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
      }

      Iterator<Pair<JsonElement, JsonElement>> valuesIterator = values.entries().iterator();
      if (!valuesIterator.hasNext()) {
         return map == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(map);
      }

      JsonObject output = new JsonObject();
      if (map != this.empty()) {
         map.getAsJsonObject().entrySet().forEach(entry -> output.add((String)entry.getKey(), (JsonElement)entry.getValue()));
      }

      List<JsonElement> missed = Lists.newArrayList();
      valuesIterator.forEachRemaining(entry -> {
         JsonElement key = entry.getFirst();
         if (key instanceof JsonPrimitive && (key.getAsJsonPrimitive().isString() || this.compressed)) {
            output.add(key.getAsString(), entry.getSecond());
         } else {
            missed.add(key);
         }
      });
      return !missed.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + missed, output) : DataResult.success(output);
   }

   public DataResult<Stream<Pair<JsonElement, JsonElement>>> getMapValues(JsonElement input) {
      return !(input instanceof JsonObject)
         ? DataResult.error(() -> "Not a JSON object: " + input)
         : DataResult.success(
            input.getAsJsonObject()
               .entrySet()
               .stream()
               .map(entry -> Pair.of(new JsonPrimitive((String)entry.getKey()), entry.getValue() instanceof JsonNull ? null : (JsonElement)entry.getValue()))
         );
   }

   public DataResult<Consumer<BiConsumer<JsonElement, JsonElement>>> getMapEntries(JsonElement input) {
      return !(input instanceof JsonObject) ? DataResult.error(() -> "Not a JSON object: " + input) : DataResult.success(c -> {
         for (Entry<String, JsonElement> entry : input.getAsJsonObject().entrySet()) {
            c.accept(this.createString(entry.getKey()), entry.getValue() instanceof JsonNull ? null : entry.getValue());
         }
      });
   }

   public DataResult<MapLike<JsonElement>> getMap(JsonElement input) {
      if (!(input instanceof JsonObject)) {
         return DataResult.error(() -> "Not a JSON object: " + input);
      }

      final JsonObject object = input.getAsJsonObject();
      return DataResult.success(new MapLike<JsonElement>() {
         @Nullable
         public JsonElement get(JsonElement key) {
            JsonElement element = object.get(key.getAsString());
            return element instanceof JsonNull ? null : element;
         }

         @Nullable
         public JsonElement get(String key) {
            JsonElement element = object.get(key);
            return element instanceof JsonNull ? null : element;
         }

         @Override
         public Stream<Pair<JsonElement, JsonElement>> entries() {
            return object.entrySet().stream().map(e -> Pair.of(new JsonPrimitive((String)e.getKey()), (JsonElement)e.getValue()));
         }

         @Override
         public String toString() {
            return "MapLike[" + object + "]";
         }
      });
   }

   public JsonElement createMap(Stream<Pair<JsonElement, JsonElement>> map) {
      JsonObject result = new JsonObject();
      map.forEach(p -> result.add(p.getFirst().getAsString(), p.getSecond()));
      return result;
   }

   public DataResult<Stream<JsonElement>> getStream(JsonElement input) {
      return input instanceof JsonArray
         ? DataResult.success(
            StreamSupport.<JsonElement>stream(input.getAsJsonArray().spliterator(), false).map(e -> (JsonElement)(e instanceof JsonNull ? null : e))
         )
         : DataResult.error(() -> "Not a json array: " + input);
   }

   public DataResult<Consumer<Consumer<JsonElement>>> getList(JsonElement input) {
      return input instanceof JsonArray ? DataResult.success(c -> {
         for (JsonElement element : input.getAsJsonArray()) {
            c.accept(element instanceof JsonNull ? null : element);
         }
      }) : DataResult.error(() -> "Not a json array: " + input);
   }

   public JsonElement createList(Stream<JsonElement> input) {
      JsonArray result = new JsonArray();
      input.forEach(result::add);
      return result;
   }

   public JsonElement remove(JsonElement input, String key) {
      if (input instanceof JsonObject) {
         JsonObject result = new JsonObject();
         input.getAsJsonObject()
            .entrySet()
            .stream()
            .filter(entry -> !Objects.equals(entry.getKey(), key))
            .forEach(entry -> result.add((String)entry.getKey(), (JsonElement)entry.getValue()));
         return result;
      } else {
         return input;
      }
   }

   @Override
   public String toString() {
      return "JSON";
   }

   @Override
   public ListBuilder<JsonElement> listBuilder() {
      return new JsonOps.ArrayBuilder();
   }

   @Override
   public boolean compressMaps() {
      return this.compressed;
   }

   @Override
   public RecordBuilder<JsonElement> mapBuilder() {
      return new JsonOps.JsonRecordBuilder();
   }

   private static final class ArrayBuilder implements ListBuilder<JsonElement> {
      private DataResult<JsonArray> builder = DataResult.success(new JsonArray(), Lifecycle.stable());

      @Override
      public DynamicOps<JsonElement> ops() {
         return JsonOps.INSTANCE;
      }

      public ListBuilder<JsonElement> add(JsonElement value) {
         this.builder = this.builder.map(b -> {
            b.add(value);
            return (JsonArray)b;
         });
         return this;
      }

      @Override
      public ListBuilder<JsonElement> add(DataResult<JsonElement> value) {
         this.builder = this.builder.apply2stable((b, element) -> {
            b.add(element);
            return b;
         }, value);
         return this;
      }

      @Override
      public ListBuilder<JsonElement> withErrorsFrom(DataResult<?> result) {
         this.builder = this.builder.flatMap(r -> result.map(v -> r));
         return this;
      }

      @Override
      public ListBuilder<JsonElement> mapError(UnaryOperator<String> onError) {
         this.builder = this.builder.mapError(onError);
         return this;
      }

      public DataResult<JsonElement> build(JsonElement prefix) {
         DataResult<JsonElement> result = this.builder.flatMap(b -> {
            if (!(prefix instanceof JsonArray) && prefix != this.ops().empty()) {
               return DataResult.error(() -> "Cannot append a list to not a list: " + prefix, prefix);
            }

            JsonArray array = new JsonArray();
            if (prefix != this.ops().empty()) {
               array.addAll(prefix.getAsJsonArray());
            }

            array.addAll(b);
            return DataResult.success(array, Lifecycle.stable());
         });
         this.builder = DataResult.success(new JsonArray(), Lifecycle.stable());
         return result;
      }
   }

   private class JsonRecordBuilder extends RecordBuilder.AbstractStringBuilder<JsonElement, JsonObject> {
      protected JsonRecordBuilder() {
         super(JsonOps.this);
      }

      protected JsonObject initBuilder() {
         return new JsonObject();
      }

      protected JsonObject append(String key, JsonElement value, JsonObject builder) {
         builder.add(key, value);
         return builder;
      }

      protected DataResult<JsonElement> build(JsonObject builder, JsonElement prefix) {
         if (prefix == null || prefix instanceof JsonNull) {
            return DataResult.success(builder);
         }

         if (!(prefix instanceof JsonObject)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + prefix, prefix);
         }

         JsonObject result = new JsonObject();

         for (Entry<String, JsonElement> entry : prefix.getAsJsonObject().entrySet()) {
            result.add(entry.getKey(), entry.getValue());
         }

         for (Entry<String, JsonElement> entry : builder.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
         }

         return DataResult.success(result);
      }
   }
}
