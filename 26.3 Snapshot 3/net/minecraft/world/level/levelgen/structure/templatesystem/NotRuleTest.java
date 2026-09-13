package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class NotRuleTest extends RuleTest {
   public static final MapCodec<NotRuleTest> CODEC = RuleTest.CODEC.fieldOf("rule").xmap(NotRuleTest::new, t -> t.rule);
   private final RuleTest rule;

   public NotRuleTest(final RuleTest rule) {
      this.rule = rule;
   }

   @Override
   public boolean test(final BlockState blockState, final BlockPos pos, final RandomSource random) {
      return !this.rule.test(blockState, pos, random);
   }

   @Override
   protected RuleTestType<?> getType() {
      return RuleTestType.NOT_TEST;
   }
}
