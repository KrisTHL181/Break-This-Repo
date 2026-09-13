package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;

public record WeatherCheck(Optional<Boolean> isRaining, Optional<Boolean> isThundering) implements LootItemCondition {
   public static final MapCodec<WeatherCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.BOOL.optionalFieldOf("raining").forGetter(WeatherCheck::isRaining),
            Codec.BOOL.optionalFieldOf("thundering").forGetter(WeatherCheck::isThundering)
         )
         .apply(i, WeatherCheck::new)
   );

   @Override
   public MapCodec<WeatherCheck> codec() {
      return MAP_CODEC;
   }

   public boolean test(final LootContext context) {
      ServerLevel level = context.getLevel();
      return this.isRaining.isPresent() && this.isRaining.get() != level.isRaining()
         ? false
         : !this.isThundering.isPresent() || this.isThundering.get() == level.isThundering();
   }

   public static WeatherCheck.Builder weather() {
      return new WeatherCheck.Builder();
   }

   public static class Builder implements LootItemCondition.Builder {
      private Optional<Boolean> isRaining = Optional.empty();
      private Optional<Boolean> isThundering = Optional.empty();

      public WeatherCheck.Builder setRaining(final boolean raining) {
         this.isRaining = Optional.of(raining);
         return this;
      }

      public WeatherCheck.Builder setThundering(final boolean thundering) {
         this.isThundering = Optional.of(thundering);
         return this;
      }

      public WeatherCheck build() {
         return new WeatherCheck(this.isRaining, this.isThundering);
      }
   }
}
