package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class RandomBlockStateMatchTest extends RuleTest {
   public static final MapCodec<RandomBlockStateMatchTest> CODEC = RecordCodecBuilder.mapCodec(
      p_74287_ -> p_74287_.group(
            BlockState.CODEC.fieldOf("block_state").forGetter(p_163770_ -> p_163770_.blockState),
            Codec.FLOAT.fieldOf("probability").forGetter(p_163768_ -> p_163768_.probability)
         )
         .apply(p_74287_, RandomBlockStateMatchTest::new)
   );
   private final BlockState blockState;
   private final float probability;

   public RandomBlockStateMatchTest(BlockState p_74280_, float p_74281_) {
      this.blockState = p_74280_;
      this.probability = p_74281_;
   }

   @Override
   public boolean test(BlockState p_230320_, RandomSource p_230321_) {
      return p_230320_ == this.blockState && p_230321_.nextFloat() < this.probability;
   }

   @Override
   protected RuleTestType<?> getType() {
      return RuleTestType.RANDOM_BLOCKSTATE_TEST;
   }
}
