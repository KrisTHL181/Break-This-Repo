package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;

public record WeatherCheck(Optional<Boolean> isRaining, Optional<Boolean> isThundering) implements LootItemCondition {
   public static final MapCodec<WeatherCheck> CODEC = RecordCodecBuilder.mapCodec(
      p_327656_ -> p_327656_.group(
            Codec.BOOL.optionalFieldOf("raining").forGetter(WeatherCheck::isRaining),
            Codec.BOOL.optionalFieldOf("thundering").forGetter(WeatherCheck::isThundering)
         )
         .apply(p_327656_, WeatherCheck::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.WEATHER_CHECK;
   }

   public boolean test(LootContext p_82066_) {
      ServerLevel serverlevel = p_82066_.getLevel();
      return this.isRaining.isPresent() && this.isRaining.get() != serverlevel.isRaining()
         ? false
         : !this.isThundering.isPresent() || this.isThundering.get() == serverlevel.isThundering();
   }

   public static WeatherCheck.Builder weather() {
      return new WeatherCheck.Builder();
   }

   public static class Builder implements LootItemCondition.Builder {
      private Optional<Boolean> isRaining = Optional.empty();
      private Optional<Boolean> isThundering = Optional.empty();

      public WeatherCheck.Builder setRaining(boolean p_298370_) {
         this.isRaining = Optional.of(p_298370_);
         return this;
      }

      public WeatherCheck.Builder setThundering(boolean p_299917_) {
         this.isThundering = Optional.of(p_299917_);
         return this;
      }

      public WeatherCheck build() {
         return new WeatherCheck(this.isRaining, this.isThundering);
      }
   }
}
