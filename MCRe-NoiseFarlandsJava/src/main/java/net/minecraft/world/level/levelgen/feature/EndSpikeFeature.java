package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.configurations.EndSpikeConfiguration;
import net.minecraft.world.phys.AABB;

public class EndSpikeFeature extends Feature<EndSpikeConfiguration> {
    public static final int NUMBER_OF_SPIKES = 10;
    private static final int SPIKE_DISTANCE = 42;
    private static final LoadingCache<Long, List<EndSpikeFeature.EndSpike>> SPIKE_CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(5L, TimeUnit.MINUTES)
        .build(new EndSpikeFeature.SpikeCacheLoader());

    public EndSpikeFeature(final Codec<EndSpikeConfiguration> codec) {
        super(codec);
    }

    public static List<EndSpikeFeature.EndSpike> getSpikesForLevel(final WorldGenLevel level) {
        RandomSource random = RandomSource.createThreadLocalInstance(level.getSeed());
        long key = random.nextLong() & 65535L;
        return SPIKE_CACHE.getUnchecked(key);
    }

    @Override
    public boolean place(final FeaturePlaceContext<EndSpikeConfiguration> context) {
        EndSpikeConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        List<EndSpikeFeature.EndSpike> spikes = config.getSpikes();
        if (spikes.isEmpty()) {
            spikes = getSpikesForLevel(level);
        }

        for (EndSpikeFeature.EndSpike spike : spikes) {
            if (spike.isCenterWithinChunk(origin)) {
                this.placeSpike(level, random, config, spike);
            }
        }

        return true;
    }

    private void placeSpike(
        final ServerLevelAccessor level, final RandomSource random, final EndSpikeConfiguration config, final EndSpikeFeature.EndSpike spike
    ) {
        int radius = spike.getRadius();

        for (BlockPos pos : BlockPos.betweenClosed(
            new BlockPos(spike.getCenterX() - radius, level.getMinY(), spike.getCenterZ() - radius),
            new BlockPos(spike.getCenterX() + radius, spike.getHeight() + 10, spike.getCenterZ() + radius)
        )) {
            if (pos.distToLowCornerSqr(spike.getCenterX(), pos.getY(), spike.getCenterZ()) <= radius * radius + 1 && pos.getY() < spike.getHeight()) {
                this.setBlock(level, pos, Blocks.OBSIDIAN.defaultBlockState());
            } else if (pos.getY() > 65) {
                this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
            }
        }

        if (spike.isGuarded()) {
            int start = -2;
            int end = 2;
            int yEnd = 3;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        boolean isXSide = Mth.abs(dx) == 2;
                        boolean isZSide = Mth.abs(dz) == 2;
                        boolean top = dy == 3;
                        if (isXSide || isZSide || top) {
                            boolean xEdge = dx == -2 || dx == 2 || top;
                            boolean zEdge = dz == -2 || dz == 2 || top;
                            BlockState state = Blocks.IRON_BARS
                                .defaultBlockState()
                                .setValue(IronBarsBlock.NORTH, xEdge && dz != -2)
                                .setValue(IronBarsBlock.SOUTH, xEdge && dz != 2)
                                .setValue(IronBarsBlock.WEST, zEdge && dx != -2)
                                .setValue(IronBarsBlock.EAST, zEdge && dx != 2);
                            this.setBlock(level, pos.set(spike.getCenterX() + dx, spike.getHeight() + dy, spike.getCenterZ() + dz), state);
                        }
                    }
                }
            }
        }

        EndCrystal endCrystal = EntityTypes.END_CRYSTAL.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (endCrystal != null) {
            endCrystal.setBeamTarget(config.getCrystalBeamTarget());
            endCrystal.setInvulnerable(config.isCrystalInvulnerable());
            endCrystal.snapTo(spike.getCenterX() + 0.5, spike.getHeight() + 1, spike.getCenterZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(endCrystal);
            BlockPos crystalPos = endCrystal.blockPosition();
            this.setBlock(level, crystalPos.below(), Blocks.BEDROCK.defaultBlockState());
            this.setBlock(level, crystalPos, FireBlock.getState(level, crystalPos));
        }
    }

    public static class EndSpike {
        public static final Codec<EndSpikeFeature.EndSpike> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.INT.optionalFieldOf("centerX", 0).forGetter(s -> s.centerX),
                    Codec.INT.optionalFieldOf("centerZ", 0).forGetter(s -> s.centerZ),
                    Codec.INT.optionalFieldOf("radius", 0).forGetter(s -> s.radius),
                    Codec.INT.optionalFieldOf("height", 0).forGetter(s -> s.height),
                    Codec.BOOL.optionalFieldOf("guarded", false).forGetter(s -> s.guarded)
                )
                .apply(i, EndSpikeFeature.EndSpike::new)
        );
        private final int centerX;
        private final int centerZ;
        private final int radius;
        private final int height;
        private final boolean guarded;
        private final AABB topBoundingBox;

        public EndSpike(final int centerX, final int centerZ, final int radius, final int height, final boolean guarded) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.height = height;
            this.guarded = guarded;
            this.topBoundingBox = new AABB(centerX - radius, DimensionType.MIN_Y, centerZ - radius, centerX + radius, DimensionType.MAX_Y, centerZ + radius);
        }

        public boolean isCenterWithinChunk(final BlockPos chunkOrigin) {
            return SectionPos.blockToSectionCoord(chunkOrigin.getX()) == SectionPos.blockToSectionCoord(this.centerX)
                && SectionPos.blockToSectionCoord(chunkOrigin.getZ()) == SectionPos.blockToSectionCoord(this.centerZ);
        }

        public int getCenterX() {
            return this.centerX;
        }

        public int getCenterZ() {
            return this.centerZ;
        }

        public int getRadius() {
            return this.radius;
        }

        public int getHeight() {
            return this.height;
        }

        public boolean isGuarded() {
            return this.guarded;
        }

        public AABB getTopBoundingBox() {
            return this.topBoundingBox;
        }
    }

    private static class SpikeCacheLoader extends CacheLoader<Long, List<EndSpikeFeature.EndSpike>> {
        public List<EndSpikeFeature.EndSpike> load(final Long seed) {
            IntArrayList sizes = Util.toShuffledList(IntStream.range(0, 10), RandomSource.createThreadLocalInstance(seed));
            List<EndSpikeFeature.EndSpike> result = Lists.newArrayList();

            for (int i = 0; i < 10; i++) {
                int x = Mth.floor(42.0 * Math.cos(2.0 * (-Math.PI + (Math.PI / 10) * i)));
                int z = Mth.floor(42.0 * Math.sin(2.0 * (-Math.PI + (Math.PI / 10) * i)));
                int size = sizes.get(i);
                int radius = 2 + size / 3;
                int height = 76 + size * 3;
                boolean guarded = size == 1 || size == 2;
                result.add(new EndSpikeFeature.EndSpike(x, z, radius, height, guarded));
            }

            return result;
        }
    }
}