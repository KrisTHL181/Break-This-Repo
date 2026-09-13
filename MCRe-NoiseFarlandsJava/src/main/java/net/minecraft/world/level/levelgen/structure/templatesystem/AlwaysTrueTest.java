package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class AlwaysTrueTest extends RuleTest {
    public static final MapCodec<AlwaysTrueTest> CODEC = MapCodec.unit(() -> AlwaysTrueTest.INSTANCE);
    public static final AlwaysTrueTest INSTANCE = new AlwaysTrueTest();

    private AlwaysTrueTest() {
    }

    @Override
    public boolean testAgainstWorldState(final LevelReader level, final BlockPos pos, final RandomSource random) {
        return true;
    }

    @Override
    public boolean test(final BlockState blockState, final RandomSource random) {
        return true;
    }

    @Override
    protected RuleTestType<?> getType() {
        return RuleTestType.ALWAYS_TRUE_TEST;
    }
}