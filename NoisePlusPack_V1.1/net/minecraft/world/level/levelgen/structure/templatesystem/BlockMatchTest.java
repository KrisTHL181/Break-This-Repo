package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMatchTest extends RuleTest {
   public static final MapCodec<BlockMatchTest> CODEC = BuiltInRegistries.BLOCK
      .byNameCodec()
      .fieldOf("block")
      .xmap(BlockMatchTest::new, p_74073_ -> p_74073_.block);
   private final Block block;

   public BlockMatchTest(Block p_74067_) {
      this.block = p_74067_;
   }

   @Override
   public boolean test(BlockState p_230277_, RandomSource p_230278_) {
      return p_230277_.is(this.block);
   }

   @Override
   protected RuleTestType<?> getType() {
      return RuleTestType.BLOCK_TEST;
   }
}
