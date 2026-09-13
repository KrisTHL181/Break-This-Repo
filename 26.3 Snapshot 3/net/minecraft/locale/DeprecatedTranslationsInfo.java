package net.minecraft.locale;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;

public record DeprecatedTranslationsInfo(List<String> removed, Map<String, String> renamed) {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DeprecatedTranslationsInfo EMPTY = new DeprecatedTranslationsInfo(List.of(), Map.of());
   public static final Codec<DeprecatedTranslationsInfo> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.listOf().fieldOf("removed").forGetter(DeprecatedTranslationsInfo::removed),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("renamed").forGetter(DeprecatedTranslationsInfo::renamed)
         )
         .apply(i, DeprecatedTranslationsInfo::new)
   );

   public static DeprecatedTranslationsInfo loadFromJson(final InputStream stream) {
      JsonElement entries = StrictJsonParser.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
      return (DeprecatedTranslationsInfo)CODEC.parse(JsonOps.INSTANCE, entries)
         .getOrThrow(msg -> new IllegalStateException("Failed to parse deprecated language data: " + msg));
   }

   public static DeprecatedTranslationsInfo loadFromResource(final String path) {
      try (InputStream stream = Language.class.getResourceAsStream(path)) {
         if (stream != null) {
            return loadFromJson(stream);
         }
      } catch (Exception e) {
         LOGGER.error("Failed to read {}", path, e);
      }

      return EMPTY;
   }

   public static DeprecatedTranslationsInfo loadFromDefaultResource() {
      return loadFromResource("/assets/minecraft/lang/deprecated.json");
   }

   public void applyToMap(final Map<String, String> translations) {
      for (String key : this.removed) {
         translations.remove(key);
      }

      this.renamed.forEach((fromKey, toKey) -> {
         String value = translations.remove(fromKey);
         if (value == null) {
            LOGGER.warn("Missing translation key for rename: {}", fromKey);
            translations.remove(toKey);
         } else {
            translations.put(toKey, value);
         }
      });
   }
}
