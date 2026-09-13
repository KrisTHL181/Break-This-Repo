package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;

public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider {
   public static final MapCodec<BinomialDistributionGenerator> CODEC = RecordCodecBuilder.mapCodec(
      p_297459_ -> p_297459_.group(
            NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n),
            NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)
         )
         .apply(p_297459_, BinomialDistributionGenerator::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.BINOMIAL;
   }

   @Override
   public int getInt(LootContext p_165663_) {
      int i = this.n.getInt(p_165663_);
      float f = this.p.getFloat(p_165663_);
      RandomSource randomsource = p_165663_.getRandom();
      int j = 0;

      for (int k = 0; k < i; k++) {
         if (randomsource.nextFloat() < f) {
            j++;
         }
      }

      return j;
   }

   @Override
   public float getFloat(LootContext p_165666_) {
      return this.getInt(p_165666_);
   }

   public static BinomialDistributionGenerator binomial(int p_165660_, float p_165661_) {
      return new BinomialDistributionGenerator(ConstantValue.exactly(p_165660_), ConstantValue.exactly(p_165661_));
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
   }
}
