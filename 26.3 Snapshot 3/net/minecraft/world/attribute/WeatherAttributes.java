package net.minecraft.world.attribute;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.attribute.modifier.FloatWithAlpha;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timelines;

public class WeatherAttributes {
   public static final EnvironmentAttributeMap RAIN = EnvironmentAttributeMap.builder()
      .modify(EnvironmentAttributes.SKY_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.6F, 0.75F))
      .modify(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.6F))
      .modify(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.24F, 0.5F))
      .modify(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(4.0F, 0.3125F))
      .modify(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.ALPHA_BLEND, ARGB.color(0.3125F, Timelines.NIGHT_SKY_LIGHT_COLOR))
      .modify(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(0.24F, 0.3125F))
      .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.0F)
      .modify(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ColorModifier.MULTIPLY_ARGB, ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.6F))
      .set(EnvironmentAttributes.BEES_STAY_IN_HIVE, true)
      .build();
   public static final EnvironmentAttributeMap THUNDER = EnvironmentAttributeMap.builder()
      .modify(EnvironmentAttributes.SKY_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.24F, 0.94F))
      .modify(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 0.3F))
      .modify(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.095F, 0.94F))
      .modify(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(4.0F, 0.52734375F))
      .modify(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.ALPHA_BLEND, ARGB.color(0.52734375F, Timelines.NIGHT_SKY_LIGHT_COLOR))
      .modify(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(0.24F, 0.52734375F))
      .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.0F)
      .modify(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ColorModifier.MULTIPLY_ARGB, ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 0.3F))
      .set(EnvironmentAttributes.BEES_STAY_IN_HIVE, true)
      .build();
   private static final Set<EnvironmentAttribute<?>> WEATHER_ATTRIBUTES = Sets.union(RAIN.keySet(), THUNDER.keySet());

   public static void addBuiltinLayers(final EnvironmentAttributeSystem.Builder system, final WeatherAttributes.WeatherAccess weatherAccess) {
      for (EnvironmentAttribute<?> attribute : WEATHER_ATTRIBUTES) {
         addLayer(system, weatherAccess, attribute);
      }
   }

   private static <Value> void addLayer(
      final EnvironmentAttributeSystem.Builder system, final WeatherAttributes.WeatherAccess weatherAccess, final EnvironmentAttribute<Value> attribute
   ) {
      EnvironmentAttributeMap.Entry<Value, ?> rainEntry = RAIN.get(attribute);
      EnvironmentAttributeMap.Entry<Value, ?> thunderEntry = THUNDER.get(attribute);
      system.addTimeBasedLayer(attribute, (result, cacheTickId) -> {
         float thunderLevel = weatherAccess.thunderLevel();
         float rainLevel = weatherAccess.rainLevel() - thunderLevel;
         if (rainEntry != null && rainLevel > 0.0F) {
            Value rainValue = rainEntry.applyModifier(result);
            result = attribute.type().stateChangeLerp().apply(rainLevel, result, rainValue);
         }

         if (thunderEntry != null && thunderLevel > 0.0F) {
            Value thunderValue = thunderEntry.applyModifier(result);
            result = attribute.type().stateChangeLerp().apply(thunderLevel, result, thunderValue);
         }

         return result;
      });
   }

   public interface WeatherAccess {
      static WeatherAttributes.WeatherAccess from(final Level level) {
         return new WeatherAttributes.WeatherAccess() {
            @Override
            public float rainLevel() {
               return level.getRainLevel(1.0F);
            }

            @Override
            public float thunderLevel() {
               return level.getThunderLevel(1.0F);
            }
         };
      }

      float rainLevel();

      float thunderLevel();
   }
}
