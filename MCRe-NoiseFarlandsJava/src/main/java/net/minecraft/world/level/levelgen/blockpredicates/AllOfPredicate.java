package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

public class AllOfPredicate extends CombiningPredicate {
    public static final MapCodec<AllOfPredicate> CODEC = codec(AllOfPredicate::new);

    public AllOfPredicate(final List<BlockPredicate> predicates) {
        super(predicates);
    }

    public boolean test(final WorldGenLevel level, final BlockPos origin) {
        for (BlockPredicate predicate : this.predicates) {
            if (!predicate.test(level, origin)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.ALL_OF;
    }
}