package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class RandomSequence {
   public static final Codec<RandomSequence> CODEC = RecordCodecBuilder.create(
      i -> i.group(XoroshiroRandomSource.CODEC.fieldOf("source").forGetter(r -> r.source)).apply(i, RandomSequence::new)
   );
   private final XoroshiroRandomSource source;

   public RandomSequence(final XoroshiroRandomSource source) {
      this.source = source;
   }

   public RandomSequence(final long seed, final Identifier key) {
      this(createSequence(seed, Optional.of(key)));
   }

   public RandomSequence(final long seed, final Optional<Identifier> key) {
      this(createSequence(seed, key));
   }

   private static XoroshiroRandomSource createSequence(final long seed, final Optional<Identifier> key) {
      RandomSupport.Seed128bit seed128bit = RandomSupport.upgradeSeedTo128bitUnmixed(seed);
      if (key.isPresent()) {
         seed128bit = seed128bit.xor(seedForKey(key.get()));
      }

      return new XoroshiroRandomSource(seed128bit.mixed());
   }

   public static RandomSupport.Seed128bit seedForKey(final Identifier key) {
      return RandomSupport.seedFromHashOf(key.toString());
   }

   public RandomSource random() {
      return this.source;
   }
}
