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
      p_368182_ -> p_368182_.group(
            Codec.STRING.listOf().fieldOf("removed").forGetter(DeprecatedTranslationsInfo::removed),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("renamed").forGetter(DeprecatedTranslationsInfo::renamed)
         )
         .apply(p_368182_, DeprecatedTranslationsInfo::new)
   );

   public static DeprecatedTranslationsInfo loadFromJson(InputStream p_366953_) {
      JsonElement jsonelement = StrictJsonParser.parse(new InputStreamReader(p_366953_, StandardCharsets.UTF_8));
      return (DeprecatedTranslationsInfo)CODEC.parse(JsonOps.INSTANCE, jsonelement)
         .getOrThrow(p_370184_ -> new IllegalStateException("Failed to parse deprecated language data: " + p_370184_));
   }

   public static DeprecatedTranslationsInfo loadFromResource(String p_369676_) {
      try (InputStream inputstream = Language.class.getResourceAsStream(p_369676_)) {
         return inputstream != null ? loadFromJson(inputstream) : EMPTY;
      } catch (Exception exception) {
         LOGGER.error("Failed to read {}", p_369676_, exception);
         return EMPTY;
      }
   }

   public static DeprecatedTranslationsInfo loadFromDefaultResource() {
      return loadFromResource("/assets/minecraft/lang/deprecated.json");
   }

   public void applyToMap(Map<String, String> p_370162_) {
      for (String s : this.removed) {
         p_370162_.remove(s);
      }

      this.renamed.forEach((p_363113_, p_364770_) -> {
         String s1 = p_370162_.remove(p_363113_);
         if (s1 == null) {
            LOGGER.warn("Missing translation key for rename: {}", p_363113_);
            p_370162_.remove(p_364770_);
         } else {
            p_370162_.put(p_364770_, s1);
         }
      });
   }
}
