package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RandomBlockMatchTest extends RuleTest {
   public static final MapCodec<RandomBlockMatchTest> CODEC = RecordCodecBuilder.mapCodec(
      p_360625_ -> p_360625_.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(p_163766_ -> p_163766_.block),
            Codec.FLOAT.fieldOf("probability").forGetter(p_163764_ -> p_163764_.probability)
         )
         .apply(p_360625_, RandomBlockMatchTest::new)
   );
   private final Block block;
   private final float probability;

   public RandomBlockMatchTest(Block p_74263_, float p_74264_) {
      this.block = p_74263_;
      this.probability = p_74264_;
   }

   @Override
   public boolean test(BlockState p_230317_, RandomSource p_230318_) {
      return p_230317_.is(this.block) && p_230318_.nextFloat() < this.probability;
   }

   @Override
   protected RuleTestType<?> getType() {
      return RuleTestType.RANDOM_BLOCK_TEST;
   }
}
