package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RuleTest {
   public static final Codec<RuleTest> CODEC = BuiltInRegistries.RULE_TEST.byNameCodec().dispatch("predicate_type", RuleTest::getType, RuleTestType::codec);

   public boolean testAgainstWorldState(final LevelReader level, final BlockPos pos, final RandomSource random) {
      return this.test(level.getBlockState(pos), pos, random);
   }

   public abstract boolean test(BlockState state, final BlockPos pos, RandomSource random);

   protected abstract RuleTestType<?> getType();

   public static RuleTest allOf(final List<RuleTest> predicates) {
      return new AllOfRuleTest(predicates);
   }

   public static RuleTest allOf(final RuleTest... predicates) {
      return allOf(List.of(predicates));
   }

   public static RuleTest anyOf(final List<RuleTest> predicates) {
      return new AnyOfRuleTest(predicates);
   }

   public static RuleTest anyOf(final RuleTest... predicates) {
      return anyOf(List.of(predicates));
   }

   public static RuleTest not(final RuleTest predicate) {
      return new NotRuleTest(predicate);
   }

   public static RuleTest either(final RuleTest condition, final RuleTest ifTrue, final RuleTest ifFalse) {
      return anyOf(allOf(condition, ifTrue), allOf(not(condition), ifFalse));
   }
}
