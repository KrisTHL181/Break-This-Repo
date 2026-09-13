package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public final class RandomSupport {
   public static final long GOLDEN_RATIO_64 = -7046029254386353131L;
   public static final long SILVER_RATIO_64 = 7640891576956012809L;
   private static final HashFunction MD5_128 = Hashing.md5();
   private static final AtomicLong SEED_UNIQUIFIER = new AtomicLong(8682522807148012L);

   @VisibleForTesting
   public static long mixStafford13(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   public static RandomSupport.Seed128bit upgradeSeedTo128bitUnmixed(final long legacySeed) {
      long lowBits = legacySeed ^ 7640891576956012809L;
      long highBits = lowBits + -7046029254386353131L;
      return new RandomSupport.Seed128bit(lowBits, highBits);
   }

   public static RandomSupport.Seed128bit upgradeSeedTo128bit(final long legacySeed) {
      return upgradeSeedTo128bitUnmixed(legacySeed).mixed();
   }

   public static RandomSupport.Seed128bit seedFromHashOf(final String input) {
      byte[] hashCode = MD5_128.hashString(input, StandardCharsets.UTF_8).asBytes();
      long hashLo = Longs.fromBytes(hashCode[0], hashCode[1], hashCode[2], hashCode[3], hashCode[4], hashCode[5], hashCode[6], hashCode[7]);
      long hashHi = Longs.fromBytes(hashCode[8], hashCode[9], hashCode[10], hashCode[11], hashCode[12], hashCode[13], hashCode[14], hashCode[15]);
      return new RandomSupport.Seed128bit(hashLo, hashHi);
   }

   public static long generateUniqueSeed() {
      return SEED_UNIQUIFIER.updateAndGet(current -> current * 1181783497276652981L) ^ System.nanoTime();
   }

   public record Seed128bit(long seedLo, long seedHi) {
      public RandomSupport.Seed128bit xor(final long lo, final long hi) {
         return new RandomSupport.Seed128bit(this.seedLo ^ lo, this.seedHi ^ hi);
      }

      public RandomSupport.Seed128bit xor(final RandomSupport.Seed128bit other) {
         return this.xor(other.seedLo, other.seedHi);
      }

      public RandomSupport.Seed128bit mixed() {
         return new RandomSupport.Seed128bit(RandomSupport.mixStafford13(this.seedLo), RandomSupport.mixStafford13(this.seedHi));
      }
   }
}
