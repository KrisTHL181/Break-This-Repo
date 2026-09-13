package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.stream.LongStream;
import net.minecraft.util.Util;

public class Xoroshiro128PlusPlus {
   private long seedLo;
   private long seedHi;
   public static final Codec<Xoroshiro128PlusPlus> CODEC = Codec.LONG_STREAM
      .comapFlatMap(seed -> Util.fixedSize(seed, 2).map(longs -> new Xoroshiro128PlusPlus(longs[0], longs[1])), r -> LongStream.of(r.seedLo, r.seedHi));

   public Xoroshiro128PlusPlus(final RandomSupport.Seed128bit seed) {
      this(seed.seedLo(), seed.seedHi());
   }

   public Xoroshiro128PlusPlus(final long seedLo, final long seedHi) {
      this.seedLo = seedLo;
      this.seedHi = seedHi;
      if ((this.seedLo | this.seedHi) == 0L) {
         this.seedLo = -7046029254386353131L;
         this.seedHi = 7640891576956012809L;
      }
   }

   public long nextLong() {
      long s0 = this.seedLo;
      long s1 = this.seedHi;
      long result = Long.rotateLeft(s0 + s1, 17) + s0;
      s1 ^= s0;
      this.seedLo = Long.rotateLeft(s0, 49) ^ s1 ^ s1 << 21;
      this.seedHi = Long.rotateLeft(s1, 28);
      return result;
   }
}
