package net.minecraft.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class GsonHelper {
   private static final Gson GSON = new GsonBuilder().create();

   public static boolean isStringValue(final JsonObject node, final String name) {
      return !isValidPrimitive(node, name) ? false : node.getAsJsonPrimitive(name).isString();
   }

   public static boolean isStringValue(final JsonElement node) {
      return !node.isJsonPrimitive() ? false : node.getAsJsonPrimitive().isString();
   }

   public static boolean isNumberValue(final JsonObject node, final String name) {
      return !isValidPrimitive(node, name) ? false : node.getAsJsonPrimitive(name).isNumber();
   }

   public static boolean isNumberValue(final JsonElement node) {
      return !node.isJsonPrimitive() ? false : node.getAsJsonPrimitive().isNumber();
   }

   public static boolean isBooleanValue(final JsonObject node, final String name) {
      return !isValidPrimitive(node, name) ? false : node.getAsJsonPrimitive(name).isBoolean();
   }

   public static boolean isBooleanValue(final JsonElement node) {
      return !node.isJsonPrimitive() ? false : node.getAsJsonPrimitive().isBoolean();
   }

   public static boolean isArrayNode(final JsonObject node, final String name) {
      return !isValidNode(node, name) ? false : node.get(name).isJsonArray();
   }

   public static boolean isObjectNode(final JsonObject node, final String name) {
      return !isValidNode(node, name) ? false : node.get(name).isJsonObject();
   }

   public static boolean isValidPrimitive(final JsonObject node, final String name) {
      return !isValidNode(node, name) ? false : node.get(name).isJsonPrimitive();
   }

   public static boolean isValidNode(final @Nullable JsonObject node, final String name) {
      return node == null ? false : node.get(name) != null;
   }

   public static JsonElement getNonNull(final JsonObject object, final String name) {
      JsonElement result = object.get(name);
      if (result != null && !result.isJsonNull()) {
         return result;
      } else {
         throw new JsonSyntaxException("Missing field " + name);
      }
   }

   public static String convertToString(final JsonElement element, final String name) {
      if (element.isJsonPrimitive()) {
         return element.getAsString();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a string, was " + getType(element));
      }
   }

   public static String getAsString(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToString(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a string");
      }
   }

   @Contract("_,_,!null->!null;_,_,null->_")
   public static @Nullable String getAsString(final JsonObject object, final String name, final @Nullable String def) {
      return object.has(name) ? convertToString(object.get(name), name) : def;
   }

   public static Holder<Item> convertToItem(final JsonElement element, final String name) {
      if (element.isJsonPrimitive()) {
         String itemName = element.getAsString();
         return BuiltInRegistries.ITEM
            .get(Identifier.parse(itemName))
            .orElseThrow(() -> new JsonSyntaxException("Expected " + name + " to be an item, was unknown string '" + itemName + "'"));
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be an item, was " + getType(element));
      }
   }

   public static Holder<Item> getAsItem(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToItem(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find an item");
      }
   }

   @Contract("_,_,!null->!null;_,_,null->_")
   public static @Nullable Holder<Item> getAsItem(final JsonObject object, final String name, final @Nullable Holder<Item> def) {
      return object.has(name) ? convertToItem(object.get(name), name) : def;
   }

   public static boolean convertToBoolean(final JsonElement element, final String name) {
      if (element.isJsonPrimitive()) {
         return element.getAsBoolean();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Boolean, was " + getType(element));
      }
   }

   public static boolean getAsBoolean(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToBoolean(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Boolean");
      }
   }

   public static boolean getAsBoolean(final JsonObject object, final String name, final boolean def) {
      return object.has(name) ? convertToBoolean(object.get(name), name) : def;
   }

   public static double convertToDouble(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsDouble();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Double, was " + getType(element));
      }
   }

   public static double getAsDouble(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToDouble(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Double");
      }
   }

   public static double getAsDouble(final JsonObject object, final String name, final double def) {
      return object.has(name) ? convertToDouble(object.get(name), name) : def;
   }

   public static float convertToFloat(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsFloat();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Float, was " + getType(element));
      }
   }

   public static float getAsFloat(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToFloat(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Float");
      }
   }

   public static float getAsFloat(final JsonObject object, final String name, final float def) {
      return object.has(name) ? convertToFloat(object.get(name), name) : def;
   }

   public static long convertToLong(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsLong();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Long, was " + getType(element));
      }
   }

   public static long getAsLong(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToLong(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Long");
      }
   }

   public static long getAsLong(final JsonObject object, final String name, final long def) {
      return object.has(name) ? convertToLong(object.get(name), name) : def;
   }

   public static int convertToInt(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsInt();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Int, was " + getType(element));
      }
   }

   public static int getAsInt(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToInt(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Int");
      }
   }

   public static int getAsInt(final JsonObject object, final String name, final int def) {
      return object.has(name) ? convertToInt(object.get(name), name) : def;
   }

   public static byte convertToByte(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsByte();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Byte, was " + getType(element));
      }
   }

   public static byte getAsByte(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToByte(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Byte");
      }
   }

   public static byte getAsByte(final JsonObject object, final String name, final byte def) {
      return object.has(name) ? convertToByte(object.get(name), name) : def;
   }

   public static char convertToCharacter(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsCharacter();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Character, was " + getType(element));
      }
   }

   public static char getAsCharacter(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToCharacter(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Character");
      }
   }

   public static char getAsCharacter(final JsonObject object, final String name, final char def) {
      return object.has(name) ? convertToCharacter(object.get(name), name) : def;
   }

   public static BigDecimal convertToBigDecimal(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsBigDecimal();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a BigDecimal, was " + getType(element));
      }
   }

   public static BigDecimal getAsBigDecimal(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToBigDecimal(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a BigDecimal");
      }
   }

   public static BigDecimal getAsBigDecimal(final JsonObject object, final String name, final BigDecimal def) {
      return object.has(name) ? convertToBigDecimal(object.get(name), name) : def;
   }

   public static BigInteger convertToBigInteger(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsBigInteger();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a BigInteger, was " + getType(element));
      }
   }

   public static BigInteger getAsBigInteger(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToBigInteger(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a BigInteger");
      }
   }

   public static BigInteger getAsBigInteger(final JsonObject object, final String name, final BigInteger def) {
      return object.has(name) ? convertToBigInteger(object.get(name), name) : def;
   }

   public static short convertToShort(final JsonElement element, final String name) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         return element.getAsShort();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a Short, was " + getType(element));
      }
   }

   public static short getAsShort(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToShort(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a Short");
      }
   }

   public static short getAsShort(final JsonObject object, final String name, final short def) {
      return object.has(name) ? convertToShort(object.get(name), name) : def;
   }

   public static JsonObject convertToJsonObject(final JsonElement element, final String name) {
      if (element.isJsonObject()) {
         return element.getAsJsonObject();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a JsonObject, was " + getType(element));
      }
   }

   public static JsonObject getAsJsonObject(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToJsonObject(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonObject");
      }
   }

   @Contract("_,_,!null->!null;_,_,null->_")
   public static @Nullable JsonObject getAsJsonObject(final JsonObject object, final String name, final @Nullable JsonObject def) {
      return object.has(name) ? convertToJsonObject(object.get(name), name) : def;
   }

   public static JsonArray convertToJsonArray(final JsonElement element, final String name) {
      if (element.isJsonArray()) {
         return element.getAsJsonArray();
      } else {
         throw new JsonSyntaxException("Expected " + name + " to be a JsonArray, was " + getType(element));
      }
   }

   public static JsonArray getAsJsonArray(final JsonObject object, final String name) {
      if (object.has(name)) {
         return convertToJsonArray(object.get(name), name);
      } else {
         throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray");
      }
   }

   @Contract("_,_,!null->!null;_,_,null->_")
   public static @Nullable JsonArray getAsJsonArray(final JsonObject object, final String name, final @Nullable JsonArray def) {
      return object.has(name) ? convertToJsonArray(object.get(name), name) : def;
   }

   public static <T> T convertToObject(
      final @Nullable JsonElement element, final String name, final JsonDeserializationContext context, final Class<? extends T> clazz
   ) {
      if (element != null) {
         return (T)context.deserialize(element, clazz);
      } else {
         throw new JsonSyntaxException("Missing " + name);
      }
   }

   public static <T> T getAsObject(final JsonObject object, final String name, final JsonDeserializationContext context, final Class<? extends T> clazz) {
      if (object.has(name)) {
         return convertToObject(object.get(name), name, context, clazz);
      } else {
         throw new JsonSyntaxException("Missing " + name);
      }
   }

   @Contract("_,_,!null,_,_->!null;_,_,null,_,_->_")
   public static <T> @Nullable T getAsObject(
      final JsonObject object, final String name, final @Nullable T def, final JsonDeserializationContext context, final Class<? extends T> clazz
   ) {
      return object.has(name) ? convertToObject(object.get(name), name, context, clazz) : def;
   }

   public static String getType(final @Nullable JsonElement element) {
      String value = StringUtils.abbreviateMiddle(String.valueOf(element), "...", 10);
      if (element == null) {
         return "null (missing)";
      }

      if (element.isJsonNull()) {
         return "null (json)";
      }

      if (element.isJsonArray()) {
         return "an array (" + value + ")";
      }

      if (element.isJsonObject()) {
         return "an object (" + value + ")";
      }

      if (element.isJsonPrimitive()) {
         JsonPrimitive primitive = element.getAsJsonPrimitive();
         if (primitive.isNumber()) {
            return "a number (" + value + ")";
         }

         if (primitive.isBoolean()) {
            return "a boolean (" + value + ")";
         }
      }

      return value;
   }

   public static <T> T fromJson(final Gson gson, final Reader reader, final Class<T> type) {
      try {
         JsonReader jsonReader = new JsonReader(reader);
         jsonReader.setStrictness(Strictness.STRICT);
         T result = (T)gson.getAdapter(type).read(jsonReader);
         if (result == null) {
            throw new JsonParseException("JSON data was null or empty");
         } else {
            return result;
         }
      } catch (IOException e) {
         throw new JsonParseException(e);
      }
   }

   public static <T> @Nullable T fromNullableJson(final Gson gson, final Reader reader, final TypeToken<T> type) {
      try {
         JsonReader jsonReader = new JsonReader(reader);
         jsonReader.setStrictness(Strictness.STRICT);
         return (T)gson.getAdapter(type).read(jsonReader);
      } catch (IOException e) {
         throw new JsonParseException(e);
      }
   }

   public static <T> T fromJson(final Gson gson, final Reader reader, final TypeToken<T> type) {
      T result = fromNullableJson(gson, reader, type);
      if (result == null) {
         throw new JsonParseException("JSON data was null or empty");
      } else {
         return result;
      }
   }

   public static <T> @Nullable T fromNullableJson(final Gson gson, final String string, final TypeToken<T> type) {
      return fromNullableJson(gson, new StringReader(string), type);
   }

   public static <T> T fromJson(final Gson gson, final String string, final Class<T> type) {
      return fromJson(gson, new StringReader(string), type);
   }

   public static JsonObject parse(final String string) {
      return parse(new StringReader(string));
   }

   public static JsonObject parse(final Reader reader) {
      return fromJson(GSON, reader, JsonObject.class);
   }

   public static JsonArray parseArray(final String string) {
      return parseArray(new StringReader(string));
   }

   public static JsonArray parseArray(final Reader reader) {
      return fromJson(GSON, reader, JsonArray.class);
   }

   public static String toStableString(final JsonElement jsonElement) {
      StringWriter stringWriter = new StringWriter();
      JsonWriter jsonWriter = new JsonWriter(stringWriter);

      try {
         writeValue(jsonWriter, jsonElement, Comparator.naturalOrder());
      } catch (IOException e) {
         throw new AssertionError(e);
      }

      return stringWriter.toString();
   }

   public static void writeValue(final JsonWriter out, final @Nullable JsonElement value, final @Nullable Comparator<String> keyComparator) throws IOException {
      if (value == null || value.isJsonNull()) {
         out.nullValue();
      } else if (value.isJsonPrimitive()) {
         JsonPrimitive primitive = value.getAsJsonPrimitive();
         if (primitive.isNumber()) {
            out.value(primitive.getAsNumber());
         } else if (primitive.isBoolean()) {
            out.value(primitive.getAsBoolean());
         } else {
            out.value(primitive.getAsString());
         }
      } else if (value.isJsonArray()) {
         out.beginArray();

         for (JsonElement e : value.getAsJsonArray()) {
            writeValue(out, e, keyComparator);
         }

         out.endArray();
      } else {
         if (!value.isJsonObject()) {
            throw new IllegalArgumentException("Couldn't write " + value.getClass());
         }

         out.beginObject();

         for (Entry<String, JsonElement> e : sortByKeyIfNeeded(value.getAsJsonObject().entrySet(), keyComparator)) {
            out.name(e.getKey());
            writeValue(out, e.getValue(), keyComparator);
         }

         out.endObject();
      }
   }

   private static Collection<Entry<String, JsonElement>> sortByKeyIfNeeded(
      final Collection<Entry<String, JsonElement>> elements, final @Nullable Comparator<String> keyComparator
   ) {
      if (keyComparator == null) {
         return elements;
      }

      List<Entry<String, JsonElement>> sorted = new ArrayList<>(elements);
      sorted.sort(Entry.comparingByKey(keyComparator));
      return sorted;
   }

   public static boolean encodesLongerThan(final JsonElement element, final int limit) {
      try {
         Streams.write(element, new JsonWriter(Streams.writerForAppendable(new GsonHelper.CountedAppendable(limit))));
         return false;
      } catch (IllegalStateException e) {
         return true;
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   private static class CountedAppendable implements Appendable {
      private int totalCount;
      private final int limit;

      public CountedAppendable(final int limit) {
         this.limit = limit;
      }

      private Appendable accountChars(final int count) {
         this.totalCount += count;
         if (this.totalCount > this.limit) {
            throw new IllegalStateException("Character count over limit: " + this.totalCount + " > " + this.limit);
         } else {
            return this;
         }
      }

      @Override
      public Appendable append(final CharSequence csq) {
         return this.accountChars(csq.length());
      }

      @Override
      public Appendable append(final CharSequence csq, final int start, final int end) {
         return this.accountChars(end - start);
      }

      @Override
      public Appendable append(final char c) {
         return this.accountChars(1);
      }
   }
}
