package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class AllOfRuleTest extends RuleTest {
   public static final MapCodec<AllOfRuleTest> CODEC = RuleTest.CODEC.listOf().fieldOf("rules").xmap(AllOfRuleTest::new, t -> t.rules);
   private final List<RuleTest> rules;

   public AllOfRuleTest(final List<RuleTest> rules) {
      this.rules = rules;
   }

   @Override
   public boolean test(final BlockState blockState, final BlockPos pos, final RandomSource random) {
      for (RuleTest rule : this.rules) {
         if (!rule.test(blockState, pos, random)) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected RuleTestType<?> getType() {
      return RuleTestType.ALL_OF_TEST;
   }
}
