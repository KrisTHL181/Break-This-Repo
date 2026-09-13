package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class LinearPosTest extends PosRuleTest {
   public static final MapCodec<LinearPosTest> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("min_chance", 0.0F).forGetter(p -> p.minChance),
            Codec.FLOAT.optionalFieldOf("max_chance", 0.0F).forGetter(p -> p.maxChance),
            Codec.INT.optionalFieldOf("min_dist", 0).forGetter(p -> p.minDist),
            Codec.INT.optionalFieldOf("max_dist", 0).forGetter(p -> p.maxDist)
         )
         .apply(i, LinearPosTest::new)
   );
   private final float minChance;
   private final float maxChance;
   private final int minDist;
   private final int maxDist;

   public LinearPosTest(final float minChance, final float maxChance, final int minDist, final int maxDist) {
      if (minDist >= maxDist) {
         throw new IllegalArgumentException("Invalid range: [" + minDist + "," + maxDist + "]");
      }

      this.minChance = minChance;
      this.maxChance = maxChance;
      this.minDist = minDist;
      this.maxDist = maxDist;
   }

   @Override
   public boolean test(final BlockPos inTemplatePos, final BlockPos worldPos, final BlockPos worldReference, final RandomSource random) {
      int dist = worldPos.distManhattan(worldReference);
      float rnd = random.nextFloat();
      return rnd <= Mth.clampedLerp(Mth.inverseLerp(dist, this.minDist, this.maxDist), this.minChance, this.maxChance);
   }

   @Override
   protected PosRuleTestType<?> getType() {
      return PosRuleTestType.LINEAR_POS_TEST;
   }
}
