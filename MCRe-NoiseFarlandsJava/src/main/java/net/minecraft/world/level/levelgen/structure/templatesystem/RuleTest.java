package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RuleTest {
    public static final Codec<RuleTest> CODEC = BuiltInRegistries.RULE_TEST.byNameCodec().dispatch("predicate_type", RuleTest::getType, RuleTestType::codec);

    public boolean testAgainstWorldState(final LevelReader level, final BlockPos pos, final RandomSource random) {
        return this.test(level.getBlockState(pos), random);
    }

    public abstract boolean test(BlockState state, RandomSource random);

    protected abstract RuleTestType<?> getType();
}