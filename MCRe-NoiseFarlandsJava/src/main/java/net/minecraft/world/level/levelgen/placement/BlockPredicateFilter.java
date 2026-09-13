package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class BlockPredicateFilter extends PlacementFilter {
    public static final MapCodec<BlockPredicateFilter> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(BlockPredicate.CODEC.fieldOf("predicate").forGetter(c -> c.predicate)).apply(i, BlockPredicateFilter::new)
    );
    private final BlockPredicate predicate;

    private BlockPredicateFilter(final BlockPredicate predicate) {
        this.predicate = predicate;
    }

    public static BlockPredicateFilter forPredicate(final BlockPredicate predicate) {
        return new BlockPredicateFilter(predicate);
    }

    @Override
    protected boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
        return this.predicate.test(context.getLevel(), origin);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.BLOCK_PREDICATE_FILTER;
    }
}