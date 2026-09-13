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
      p_287586_ -> p_287586_.group(XoroshiroRandomSource.CODEC.fieldOf("source").forGetter(p_287757_ -> p_287757_.source))
         .apply(p_287586_, RandomSequence::new)
   );
   private final XoroshiroRandomSource source;

   public RandomSequence(XoroshiroRandomSource p_287597_) {
      this.source = p_287597_;
   }

   public RandomSequence(long p_287592_, Identifier p_456657_) {
      this(createSequence(p_287592_, Optional.of(p_456657_)));
   }

   public RandomSequence(long p_298200_, Optional<Identifier> p_297536_) {
      this(createSequence(p_298200_, p_297536_));
   }

   private static XoroshiroRandomSource createSequence(long p_289567_, Optional<Identifier> p_300474_) {
      RandomSupport.Seed128bit randomsupport$seed128bit = RandomSupport.upgradeSeedTo128bitUnmixed(p_289567_);
      if (p_300474_.isPresent()) {
         randomsupport$seed128bit = randomsupport$seed128bit.xor(seedForKey(p_300474_.get()));
      }

      return new XoroshiroRandomSource(randomsupport$seed128bit.mixed());
   }

   public static RandomSupport.Seed128bit seedForKey(Identifier p_459531_) {
      return RandomSupport.seedFromHashOf(p_459531_.toString());
   }

   public RandomSource random() {
      return this.source;
   }
}
