package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class WouldSurvivePredicate implements BlockPredicate {
    public static final MapCodec<WouldSurvivePredicate> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Vec3i.offsetCodec(16).optionalFieldOf("offset", Vec3i.ZERO).forGetter(c -> c.offset), BlockState.CODEC.fieldOf("state").forGetter(c -> c.state)
            )
            .apply(i, WouldSurvivePredicate::new)
    );
    private final Vec3i offset;
    private final BlockState state;

    protected WouldSurvivePredicate(final Vec3i offset, final BlockState state) {
        this.offset = offset;
        this.state = state;
    }

    public boolean test(final WorldGenLevel level, final BlockPos origin) {
        return this.state.canSurvive(level, origin.offset(this.offset));
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.WOULD_SURVIVE;
    }
}