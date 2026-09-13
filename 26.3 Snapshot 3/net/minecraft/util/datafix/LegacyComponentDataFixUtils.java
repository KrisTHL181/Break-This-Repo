package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StrictJsonParser;

public class LegacyComponentDataFixUtils {
   private static final String EMPTY_CONTENTS = createTextComponentJson("");

   public static <T> Dynamic<T> createPlainTextComponent(final DynamicOps<T> ops, final String text) {
      String stableString = createTextComponentJson(text);
      return new Dynamic(ops, ops.createString(stableString));
   }

   public static <T> Dynamic<T> createEmptyComponent(final DynamicOps<T> ops) {
      return new Dynamic(ops, ops.createString(EMPTY_CONTENTS));
   }

   public static String createTextComponentJson(final String text) {
      JsonObject result = new JsonObject();
      result.addProperty("text", text);
      return GsonHelper.toStableString(result);
   }

   public static String createTranslatableComponentJson(final String key) {
      JsonObject result = new JsonObject();
      result.addProperty("translate", key);
      return GsonHelper.toStableString(result);
   }

   public static <T> Dynamic<T> createTranslatableComponent(final DynamicOps<T> ops, final String key) {
      String stableString = createTranslatableComponentJson(key);
      return new Dynamic(ops, ops.createString(stableString));
   }

   public static String rewriteFromLenient(final String string) {
      if (!string.isEmpty() && !string.equals("null")) {
         char firstChar = string.charAt(0);
         char lastChar = string.charAt(string.length() - 1);
         if (firstChar == '"' && lastChar == '"' || firstChar == '{' && lastChar == '}' || firstChar == '[' && lastChar == ']') {
            try {
               JsonElement json = LenientJsonParser.parse(string);
               if (json.isJsonPrimitive()) {
                  return createTextComponentJson(json.getAsString());
               }

               return GsonHelper.toStableString(json);
            } catch (JsonParseException var4) {
            }
         }

         return createTextComponentJson(string);
      } else {
         return EMPTY_CONTENTS;
      }
   }

   public static boolean isStrictlyValidJson(final Dynamic<?> component) {
      return component.asString().result().filter(string -> {
         try {
            StrictJsonParser.parse(string);
            return true;
         } catch (JsonParseException ignored) {
            return false;
         }
      }).isPresent();
   }

   public static Optional<String> extractTranslationString(final String component) {
      try {
         JsonElement parsed = LenientJsonParser.parse(component);
         if (parsed.isJsonObject()) {
            JsonObject parsedObject = parsed.getAsJsonObject();
            JsonElement key = parsedObject.get("translate");
            if (key != null && key.isJsonPrimitive()) {
               return Optional.of(key.getAsString());
            }
         }
      } catch (JsonParseException var4) {
      }

      return Optional.empty();
   }
}
