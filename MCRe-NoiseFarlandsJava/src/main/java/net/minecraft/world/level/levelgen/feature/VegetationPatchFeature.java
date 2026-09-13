package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
    public VegetationPatchFeature(final Codec<VegetationPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<VegetationPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        VegetationPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        Predicate<BlockState> replaceable = s -> s.is(config.replaceable());
        int xRadius = config.xzRadius().sample(random) + 1;
        int zRadius = config.xzRadius().sample(random) + 1;
        Set<BlockPos> surface = this.placeGroundPatch(level, config, random, origin, replaceable, xRadius, zRadius);
        this.distributeVegetation(context, level, config, random, surface, xRadius, zRadius);
        return !surface.isEmpty();
    }

    protected Set<BlockPos> placeGroundPatch(
        final WorldGenLevel level,
        final VegetationPatchConfiguration config,
        final RandomSource random,
        final BlockPos origin,
        final Predicate<BlockState> replaceable,
        final int xRadius,
        final int zRadius
    ) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        BlockPos.MutableBlockPos belowPos = pos.mutable();
        Direction inwards = config.surface().getDirection();
        Direction outwards = inwards.getOpposite();
        Set<BlockPos> surface = new HashSet<>();

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            boolean isXEdge = dx == -xRadius || dx == xRadius;

            for (int dz = -zRadius; dz <= zRadius; dz++) {
                boolean isZEdge = dz == -zRadius || dz == zRadius;
                boolean isEdge = isXEdge || isZEdge;
                boolean isCorner = isXEdge && isZEdge;
                boolean isEdgeButNotCorner = isEdge && !isCorner;
                if (!isCorner && (!isEdgeButNotCorner || config.extraEdgeColumnChance() != 0.0F && !(random.nextFloat() > config.extraEdgeColumnChance()))) {
                    pos.setWithOffset(origin, dx, 0, dz);

                    for (int offset = 0; level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::isAir) && offset < config.verticalRange(); offset++) {
                        pos.move(inwards);
                    }

                    for (int var25 = 0; level.isStateAtPosition(pos, s -> !s.isAir()) && var25 < config.verticalRange(); var25++) {
                        pos.move(outwards);
                    }

                    belowPos.setWithOffset(pos, config.surface().getDirection());
                    BlockState belowState = level.getBlockState(belowPos);
                    if (level.isEmptyBlock(pos) && belowState.isFaceSturdy(level, belowPos, config.surface().getDirection().getOpposite())) {
                        int depth = config.depth().sample(random)
                            + (config.extraBottomBlockChance() > 0.0F && random.nextFloat() < config.extraBottomBlockChance() ? 1 : 0);
                        BlockPos groundPos = belowPos.immutable();
                        boolean groundPlaced = this.placeGround(level, config, replaceable, random, belowPos, depth);
                        if (groundPlaced) {
                            surface.add(groundPos);
                        }
                    }
                }
            }
        }

        return surface;
    }

    protected void distributeVegetation(
        final FeaturePlaceContext<VegetationPatchConfiguration> context,
        final WorldGenLevel level,
        final VegetationPatchConfiguration config,
        final RandomSource random,
        final Set<BlockPos> surface,
        final int xRadius,
        final int zRadius
    ) {
        for (BlockPos surfacePos : surface) {
            if (config.vegetationChance() > 0.0F && random.nextFloat() < config.vegetationChance()) {
                this.placeVegetation(level, config, context.chunkGenerator(), random, surfacePos);
            }
        }
    }

    protected boolean placeVegetation(
        final WorldGenLevel level,
        final VegetationPatchConfiguration config,
        final ChunkGenerator generator,
        final RandomSource random,
        final BlockPos vegetationPos
    ) {
        return config.vegetationFeature().value().place(level, generator, random, vegetationPos.relative(config.surface().getDirection().getOpposite()));
    }

    protected boolean placeGround(
        final WorldGenLevel level,
        final VegetationPatchConfiguration config,
        final Predicate<BlockState> replaceable,
        final RandomSource random,
        final BlockPos.MutableBlockPos belowPos,
        final int depth
    ) {
        for (int i = 0; i < depth; i++) {
            BlockState stateToPlace = config.groundState().getState(level, random, belowPos);
            BlockState belowState = level.getBlockState(belowPos);
            if (!stateToPlace.is(belowState.getBlock())) {
                if (!replaceable.test(belowState)) {
                    return i != 0;
                }

                level.setBlock(belowPos, stateToPlace, 2);
                belowPos.move(config.surface().getDirection());
            }
        }

        return true;
    }
}