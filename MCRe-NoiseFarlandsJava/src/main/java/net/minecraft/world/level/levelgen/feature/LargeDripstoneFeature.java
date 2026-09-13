package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LargeDripstoneFeature extends Feature<LargeDripstoneConfiguration> {
    public LargeDripstoneFeature(final Codec<LargeDripstoneConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<LargeDripstoneConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        LargeDripstoneConfiguration config = context.config();
        RandomSource random = context.random();
        if (!SpeleothemUtils.isEmptyOrWater(level, origin)) {
            return false;
        }

        Optional<Column> column = Column.scan(
            level,
            origin,
            config.floorToCeilingSearchRange,
            SpeleothemUtils::isEmptyOrWater,
            state -> SpeleothemUtils.isBaseOrLava(state, Blocks.DRIPSTONE_BLOCK, config.replaceableBlocks)
        );
        if (!(!column.isEmpty() && column.get() instanceof Column.Range columnRange)) {
            return false;
        }

        if (columnRange.height() < 4) {
            return false;
        }

        int maxColumnRadiusBasedOnColumnHeight = (int)(columnRange.height() * config.maxColumnRadiusToCaveHeightRatio);
        int maxColumnRadius = Mth.clamp(maxColumnRadiusBasedOnColumnHeight, config.columnRadius.minInclusive(), config.columnRadius.maxInclusive());
        int radius = Mth.randomBetweenInclusive(random, config.columnRadius.minInclusive(), maxColumnRadius);
        LargeDripstoneFeature.LargeDripstone stalactite = makeDripstone(
            origin.atY(columnRange.ceiling() - 1), false, random, radius, config.stalactiteBluntness, config.heightScale
        );
        LargeDripstoneFeature.LargeDripstone stalagmite = makeDripstone(
            origin.atY(columnRange.floor() + 1), true, random, radius, config.stalagmiteBluntness, config.heightScale
        );
        LargeDripstoneFeature.WindOffsetter wind;
        if (stalactite.isSuitableForWind(config) && stalagmite.isSuitableForWind(config)) {
            wind = new LargeDripstoneFeature.WindOffsetter(origin.getY(), random, config.windSpeed, 16 - radius);
        } else {
            wind = LargeDripstoneFeature.WindOffsetter.noWind();
        }

        boolean stalactiteBaseEmbeddedInStone = stalactite.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(level, wind);
        boolean stalagmiteBaseEmbeddedInStone = stalagmite.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(level, wind);
        if (stalactiteBaseEmbeddedInStone) {
            stalactite.placeBlocks(level, random, wind);
        }

        if (stalagmiteBaseEmbeddedInStone) {
            stalagmite.placeBlocks(level, random, wind);
        }

        if (SharedConstants.DEBUG_LARGE_DRIPSTONE) {
            this.placeDebugMarkers(level, origin, columnRange, wind);
        }

        return true;
    }

    private static LargeDripstoneFeature.LargeDripstone makeDripstone(
        final BlockPos root,
        final boolean pointingUp,
        final RandomSource random,
        final int radius,
        final FloatProvider bluntness,
        final FloatProvider heightScale
    ) {
        return new LargeDripstoneFeature.LargeDripstone(root, pointingUp, radius, bluntness.sample(random), heightScale.sample(random));
    }

    private void placeDebugMarkers(final WorldGenLevel level, final BlockPos origin, final Column.Range range, final LargeDripstoneFeature.WindOffsetter wind) {
        level.setBlock(wind.offset(origin.atY(range.ceiling() - 1)), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        level.setBlock(wind.offset(origin.atY(range.floor() + 1)), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

        for (BlockPos.MutableBlockPos pos = origin.atY(range.floor() + 2).mutable(); pos.getY() < range.ceiling() - 1; pos.move(Direction.UP)) {
            BlockPos windAdjustedPos = wind.offset(pos);
            if (SpeleothemUtils.isEmptyOrWater(level, windAdjustedPos) || level.getBlockState(windAdjustedPos).is(Blocks.DRIPSTONE_BLOCK)) {
                level.setBlock(windAdjustedPos, Blocks.CREEPER_HEAD.defaultBlockState(), 2);
            }
        }
    }

    private static final class LargeDripstone {
        private BlockPos root;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;

        private LargeDripstone(final BlockPos root, final boolean pointingUp, final int radius, final double bluntness, final double scale) {
            this.root = root;
            this.pointingUp = pointingUp;
            this.radius = radius;
            this.bluntness = bluntness;
            this.scale = scale;
        }

        private int getHeight() {
            return this.getHeightAtRadius(0.0F);
        }

        private int getMinY() {
            return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
        }

        private int getMaxY() {
            return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
        }

        private boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(final WorldGenLevel level, final LargeDripstoneFeature.WindOffsetter wind) {
            while (this.radius > 1) {
                BlockPos.MutableBlockPos newRoot = this.root.mutable();
                int maxTries = Math.min(10, this.getHeight());

                for (int i = 0; i < maxTries; i++) {
                    if (level.getBlockState(newRoot).is(Blocks.LAVA)) {
                        return false;
                    }

                    if (SpeleothemUtils.isCircleMostlyEmbeddedInStone(level, wind.offset(newRoot), this.radius)) {
                        this.root = newRoot;
                        return true;
                    }

                    newRoot.move(this.pointingUp ? Direction.DOWN : Direction.UP);
                }

                this.radius /= 2;
            }

            return false;
        }

        private int getHeightAtRadius(final float checkRadius) {
            return (int)SpeleothemUtils.getSpeleothemHeight(checkRadius, this.radius, this.scale, this.bluntness);
        }

        private void placeBlocks(final WorldGenLevel level, final RandomSource random, final LargeDripstoneFeature.WindOffsetter wind) {
            for (int dx = -this.radius; dx <= this.radius; dx++) {
                for (int dz = -this.radius; dz <= this.radius; dz++) {
                    float currentRadius = Mth.sqrt(dx * dx + dz * dz);
                    if (!(currentRadius > this.radius)) {
                        int height = this.getHeightAtRadius(currentRadius);
                        if (height > 0) {
                            if (random.nextFloat() < 0.2) {
                                height = (int)(height * Mth.randomBetween(random, 0.8F, 1.0F));
                            }

                            BlockPos.MutableBlockPos pos = this.root.offset(dx, 0, dz).mutable();
                            boolean hasBeenOutOfStone = false;
                            int maxY = this.pointingUp ? level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) : Integer.MAX_VALUE;

                            for (int i = 0; i < height && pos.getY() < maxY; i++) {
                                BlockPos windAdjustedPos = wind.offset(pos);
                                if (SpeleothemUtils.isEmptyOrWaterOrLava(level, windAdjustedPos)) {
                                    hasBeenOutOfStone = true;
                                    Block block = SharedConstants.DEBUG_LARGE_DRIPSTONE ? Blocks.GLASS : Blocks.DRIPSTONE_BLOCK;
                                    level.setBlock(windAdjustedPos, block.defaultBlockState(), 2);
                                } else if (hasBeenOutOfStone && level.getBlockState(windAdjustedPos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                                    break;
                                }

                                pos.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                            }
                        }
                    }
                }
            }
        }

        private boolean isSuitableForWind(final LargeDripstoneConfiguration config) {
            return this.radius >= config.minRadiusForWind && this.bluntness >= config.minBluntnessForWind;
        }
    }

    private static final class WindOffsetter {
        private final int originY;
        private final @Nullable Vec3 windSpeed;
        private final int maxOffset;

        private WindOffsetter(final int originY, final RandomSource random, final FloatProvider windSpeedRange, final int maxOffset) {
            this.originY = originY;
            this.maxOffset = maxOffset;
            float speed = windSpeedRange.sample(random);
            float direction = Mth.randomBetween(random, 0.0F, (float) Math.PI);
            this.windSpeed = new Vec3(Mth.cos(direction) * speed, 0.0, Mth.sin(direction) * speed);
        }

        private WindOffsetter() {
            this.originY = 0;
            this.windSpeed = null;
            this.maxOffset = 0;
        }

        private static LargeDripstoneFeature.WindOffsetter noWind() {
            return new LargeDripstoneFeature.WindOffsetter();
        }

        private BlockPos offset(final BlockPos pos) {
            if (this.windSpeed == null) {
                return pos;
            }

            int dy = this.originY - pos.getY();
            Vec3 totalWindAdjust = this.windSpeed.scale(dy);
            int dx = Mth.clamp(Mth.floor(totalWindAdjust.x), -this.maxOffset, this.maxOffset);
            int dz = Mth.clamp(Mth.floor(totalWindAdjust.z), -this.maxOffset, this.maxOffset);
            return pos.offset(dx, 0, dz);
        }
    }
}