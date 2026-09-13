package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class AxisAlignedLinearPosTest extends PosRuleTest {
   public static final MapCodec<AxisAlignedLinearPosTest> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("min_chance", 0.0F).forGetter(p -> p.minChance),
            Codec.FLOAT.optionalFieldOf("max_chance", 0.0F).forGetter(p -> p.maxChance),
            Codec.INT.optionalFieldOf("min_dist", 0).forGetter(p -> p.minDist),
            Codec.INT.optionalFieldOf("max_dist", 0).forGetter(p -> p.maxDist),
            Direction.Axis.CODEC.optionalFieldOf("axis", Direction.Axis.Y).forGetter(p -> p.axis)
         )
         .apply(i, AxisAlignedLinearPosTest::new)
   );
   private final float minChance;
   private final float maxChance;
   private final int minDist;
   private final int maxDist;
   private final Direction.Axis axis;

   public AxisAlignedLinearPosTest(final float minChance, final float maxChance, final int minDist, final int maxDist, final Direction.Axis axis) {
      if (minDist >= maxDist) {
         throw new IllegalArgumentException("Invalid range: [" + minDist + "," + maxDist + "]");
      }

      this.minChance = minChance;
      this.maxChance = maxChance;
      this.minDist = minDist;
      this.maxDist = maxDist;
      this.axis = axis;
   }

   @Override
   public boolean test(final BlockPos inTemplatePos, final BlockPos worldPos, final BlockPos worldReference, final RandomSource random) {
      Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, this.axis);
      float xd = Math.abs((worldPos.getX() - worldReference.getX()) * direction.getStepX());
      float yd = Math.abs((worldPos.getY() - worldReference.getY()) * direction.getStepY());
      float zd = Math.abs((worldPos.getZ() - worldReference.getZ()) * direction.getStepZ());
      int dist = (int)(xd + yd + zd);
      float rnd = random.nextFloat();
      return rnd <= Mth.clampedLerp(Mth.inverseLerp(dist, this.minDist, this.maxDist), this.minChance, this.maxChance);
   }

   @Override
   protected PosRuleTestType<?> getType() {
      return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
   }
}
