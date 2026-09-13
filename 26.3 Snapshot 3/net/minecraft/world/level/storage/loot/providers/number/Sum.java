package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public record Sum(List<NumberProvider> summands) implements NumberProvider {
   public static final MapCodec<Sum> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(NumberProviders.DIRECT_CODEC.listOf().fieldOf("summands").forGetter(Sum::summands)).apply(i, Sum::new)
   );

   public static Sum sum(final NumberProvider... summands) {
      return new Sum(List.of(summands));
   }

   @Override
   public MapCodec<Sum> codec() {
      return MAP_CODEC;
   }

   @Override
   public int getInt(final LootContext context) {
      float value = 0.0F;

      for (NumberProvider provider : this.summands) {
         value += provider.getFloat(context);
      }

      return Mth.floor(value);
   }

   @Override
   public float getFloat(final LootContext context) {
      float value = 0.0F;

      for (NumberProvider provider : this.summands) {
         value += provider.getFloat(context);
      }

      return value;
   }

   @Override
   public void validate(final ValidationContext context) {
      NumberProvider.super.validate(context);
      Validatable.validate(context, "summands", this.summands);
   }
}
