package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

public class NotPredicate implements BlockPredicate {
    public static final MapCodec<NotPredicate> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(BlockPredicate.CODEC.fieldOf("predicate").forGetter(p -> p.predicate)).apply(i, NotPredicate::new)
    );
    private final BlockPredicate predicate;

    public NotPredicate(final BlockPredicate predicate) {
        this.predicate = predicate;
    }

    public boolean test(final WorldGenLevel level, final BlockPos origin) {
        return !this.predicate.test(level, origin);
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.NOT;
    }
}