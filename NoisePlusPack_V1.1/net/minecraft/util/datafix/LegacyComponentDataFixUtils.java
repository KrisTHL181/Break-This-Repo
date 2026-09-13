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

   public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> p_391511_, String p_396456_) {
      String s = createTextComponentJson(p_396456_);
      return new Dynamic(p_391511_, p_391511_.createString(s));
   }

   public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> p_396181_) {
      return new Dynamic(p_396181_, p_396181_.createString(EMPTY_CONTENTS));
   }

   public static String createTextComponentJson(String p_391436_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("text", p_391436_);
      return GsonHelper.toStableString(jsonobject);
   }

   public static String createTranslatableComponentJson(String p_394703_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("translate", p_394703_);
      return GsonHelper.toStableString(jsonobject);
   }

   public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> p_394292_, String p_392337_) {
      String s = createTranslatableComponentJson(p_392337_);
      return new Dynamic(p_394292_, p_394292_.createString(s));
   }

   public static String rewriteFromLenient(String p_395237_) {
      if (!p_395237_.isEmpty() && !p_395237_.equals("null")) {
         char c0 = p_395237_.charAt(0);
         char c1 = p_395237_.charAt(p_395237_.length() - 1);
         if (c0 == '"' && c1 == '"' || c0 == '{' && c1 == '}' || c0 == '[' && c1 == ']') {
            try {
               JsonElement jsonelement = LenientJsonParser.parse(p_395237_);
               if (jsonelement.isJsonPrimitive()) {
                  return createTextComponentJson(jsonelement.getAsString());
               }

               return GsonHelper.toStableString(jsonelement);
            } catch (JsonParseException var4) {
            }
         }

         return createTextComponentJson(p_395237_);
      } else {
         return EMPTY_CONTENTS;
      }
   }

   public static boolean isStrictlyValidJson(Dynamic<?> p_427944_) {
      return p_427944_.asString().result().filter(p_428285_ -> {
         try {
            StrictJsonParser.parse(p_428285_);
            return true;
         } catch (JsonParseException jsonparseexception) {
            return false;
         }
      }).isPresent();
   }

   public static Optional<String> extractTranslationString(String p_396189_) {
      try {
         JsonElement jsonelement = LenientJsonParser.parse(p_396189_);
         if (jsonelement.isJsonObject()) {
            JsonObject jsonobject = jsonelement.getAsJsonObject();
            JsonElement jsonelement1 = jsonobject.get("translate");
            if (jsonelement1 != null && jsonelement1.isJsonPrimitive()) {
               return Optional.of(jsonelement1.getAsString());
            }
         }
      } catch (JsonParseException var4) {
      }

      return Optional.empty();
   }
}
