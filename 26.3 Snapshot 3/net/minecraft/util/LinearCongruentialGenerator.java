package net.minecraft.util;

public class LinearCongruentialGenerator {
   private static final long MULTIPLIER = 6364136223846793005L;
   private static final long INCREMENT = 1442695040888963407L;

   public static long next(long rval, final long c) {
      rval *= rval * 6364136223846793005L + 1442695040888963407L;
      return rval + c;
   }
}
