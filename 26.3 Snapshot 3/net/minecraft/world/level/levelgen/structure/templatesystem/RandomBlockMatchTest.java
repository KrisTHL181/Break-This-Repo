package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RandomBlockMatchTest extends RuleTest {
   public static final MapCodec<RandomBlockMatchTest> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(t -> t.block), Codec.FLOAT.fieldOf("probability").forGetter(t -> t.probability)
         )
         .apply(i, RandomBlockMatchTest::new)
   );
   private final Block block;
   private final float probability;

   public RandomBlockMatchTest(final Block block, final float probability) {
      this.block = block;
      this.probability = probability;
   }

   @Override
   public boolean test(final BlockState blockState, final BlockPos pos, final RandomSource random) {
      return blockState.is(this.block) && random.nextFloat() < this.probability;
   }

   @Override
   protected RuleTestType<?> getType() {
      return RuleTestType.RANDOM_BLOCK_TEST;
   }
}
