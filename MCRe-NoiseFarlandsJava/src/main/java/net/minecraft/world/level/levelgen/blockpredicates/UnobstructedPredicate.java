package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.phys.shapes.Shapes;

public record UnobstructedPredicate(Vec3i offset) implements BlockPredicate {
    public static final MapCodec<UnobstructedPredicate> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(UnobstructedPredicate::offset)).apply(i, UnobstructedPredicate::new)
    );

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.UNOBSTRUCTED;
    }

    public boolean test(final WorldGenLevel worldGenLevel, final BlockPos pos) {
        return worldGenLevel.isUnobstructed(null, Shapes.block().move(pos));
    }
}