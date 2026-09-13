package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public interface RuleTestType<P extends RuleTest> {
   RuleTestType<AllOfRuleTest> ALL_OF_TEST = register("all_of", AllOfRuleTest.CODEC);
   RuleTestType<AlwaysTrueTest> ALWAYS_TRUE_TEST = register("always_true", AlwaysTrueTest.CODEC);
   RuleTestType<AnyOfRuleTest> ANY_OF_TEST = register("any_of", AnyOfRuleTest.CODEC);
   RuleTestType<BlockMatchTest> BLOCK_TEST = register("block_match", BlockMatchTest.CODEC);
   RuleTestType<BlockStateMatchTest> BLOCKSTATE_TEST = register("blockstate_match", BlockStateMatchTest.CODEC);
   RuleTestType<NotRuleTest> NOT_TEST = register("not", NotRuleTest.CODEC);
   RuleTestType<TagMatchTest> TAG_TEST = register("tag_match", TagMatchTest.CODEC);
   RuleTestType<HeightMatchTest> HEIGHT_TEST = register("height_match", HeightMatchTest.CODEC);
   RuleTestType<RandomBlockMatchTest> RANDOM_BLOCK_TEST = register("random_block_match", RandomBlockMatchTest.CODEC);
   RuleTestType<RandomBlockStateMatchTest> RANDOM_BLOCKSTATE_TEST = register("random_blockstate_match", RandomBlockStateMatchTest.CODEC);

   MapCodec<P> codec();

   static <P extends RuleTest> RuleTestType<P> register(final String id, final MapCodec<P> codec) {
      return Registry.register(BuiltInRegistries.RULE_TEST, id, () -> codec);
   }
}
