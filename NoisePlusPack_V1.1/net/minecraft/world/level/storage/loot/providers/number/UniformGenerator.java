package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;

public record UniformGenerator(NumberProvider min, NumberProvider max) implements NumberProvider {
   public static final MapCodec<UniformGenerator> CODEC = RecordCodecBuilder.mapCodec(
      p_299644_ -> p_299644_.group(
            NumberProviders.CODEC.fieldOf("min").forGetter(UniformGenerator::min), NumberProviders.CODEC.fieldOf("max").forGetter(UniformGenerator::max)
         )
         .apply(p_299644_, UniformGenerator::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.UNIFORM;
   }

   public static UniformGenerator between(float p_165781_, float p_165782_) {
      return new UniformGenerator(ConstantValue.exactly(p_165781_), ConstantValue.exactly(p_165782_));
   }

   @Override
   public int getInt(LootContext p_165784_) {
      return Mth.nextInt(p_165784_.getRandom(), this.min.getInt(p_165784_), this.max.getInt(p_165784_));
   }

   @Override
   public float getFloat(LootContext p_165787_) {
      return Mth.nextFloat(p_165787_.getRandom(), this.min.getFloat(p_165787_), this.max.getFloat(p_165787_));
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Sets.union(this.min.getReferencedContextParams(), this.max.getReferencedContextParams());
   }
}
