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
      p_73977_ -> p_73977_.group(
            Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter(p_163719_ -> p_163719_.minChance),
            Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter(p_163717_ -> p_163717_.maxChance),
            Codec.INT.fieldOf("min_dist").orElse(0).forGetter(p_163715_ -> p_163715_.minDist),
            Codec.INT.fieldOf("max_dist").orElse(0).forGetter(p_163713_ -> p_163713_.maxDist),
            Direction.Axis.CODEC.fieldOf("axis").orElse(Direction.Axis.Y).forGetter(p_163711_ -> p_163711_.axis)
         )
         .apply(p_73977_, AxisAlignedLinearPosTest::new)
   );
   private final float minChance;
   private final float maxChance;
   private final int minDist;
   private final int maxDist;
   private final Direction.Axis axis;

   public AxisAlignedLinearPosTest(float p_73970_, float p_73971_, int p_73972_, int p_73973_, Direction.Axis p_73974_) {
      if (p_73972_ >= p_73973_) {
         throw new IllegalArgumentException("Invalid range: [" + p_73972_ + "," + p_73973_ + "]");
      }

      this.minChance = p_73970_;
      this.maxChance = p_73971_;
      this.minDist = p_73972_;
      this.maxDist = p_73973_;
      this.axis = p_73974_;
   }

   @Override
   public boolean test(BlockPos p_230251_, BlockPos p_230252_, BlockPos p_230253_, RandomSource p_230254_) {
      Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, this.axis);
      float f = Math.abs((p_230252_.getX() - p_230253_.getX()) * direction.getStepX());
      float f1 = Math.abs((p_230252_.getY() - p_230253_.getY()) * direction.getStepY());
      float f2 = Math.abs((p_230252_.getZ() - p_230253_.getZ()) * direction.getStepZ());
      int i = (int)(f + f1 + f2);
      float f3 = p_230254_.nextFloat();
      return f3 <= Mth.clampedLerp(Mth.inverseLerp(i, this.minDist, this.maxDist), this.minChance, this.maxChance);
   }

   @Override
   protected PosRuleTestType<?> getType() {
      return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
   }
}
