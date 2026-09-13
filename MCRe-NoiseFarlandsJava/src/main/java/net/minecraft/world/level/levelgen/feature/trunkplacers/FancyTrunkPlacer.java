package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class FancyTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<FancyTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, FancyTrunkPlacer::new));
    private static final double TRUNK_HEIGHT_SCALE = 0.618;
    private static final double CLUSTER_DENSITY_MAGIC = 1.382;
    private static final double BRANCH_SLOPE = 0.381;
    private static final double BRANCH_LENGTH_MAGIC = 0.328;

    public FancyTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.FANCY_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(
        final WorldGenLevel level,
        final BiConsumer<BlockPos, BlockState> trunkSetter,
        final RandomSource random,
        final int treeHeight,
        final BlockPos origin,
        final TreeConfiguration config
    ) {
        int assumedFoliageHeight = 5;
        int height = treeHeight + 2;
        int trunkHeight = Mth.floor(height * 0.618);
        placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), config);
        double foliageDensity = 1.0;
        int clustersPerY = Math.min(1, Mth.floor(1.382 + Math.pow(1.0 * height / 13.0, 2.0)));
        int trunkTop = origin.getY() + trunkHeight;
        int relativeY = height - 5;
        List<FancyTrunkPlacer.FoliageCoords> foliageCoords = Lists.newArrayList();
        foliageCoords.add(new FancyTrunkPlacer.FoliageCoords(origin.above(relativeY), trunkTop));

        for (; relativeY >= 0; relativeY--) {
            float treeShape = treeShape(height, relativeY);
            if (!(treeShape < 0.0F)) {
                for (int i = 0; i < clustersPerY; i++) {
                    double widthScale = 1.0;
                    double radius = 1.0 * treeShape * (random.nextFloat() + 0.328);
                    double angle = random.nextFloat() * 2.0F * Math.PI;
                    double x = radius * Math.sin(angle) + 0.5;
                    double z = radius * Math.cos(angle) + 0.5;
                    BlockPos checkStart = origin.offset(Mth.floor(x), relativeY - 1, Mth.floor(z));
                    BlockPos checkEnd = checkStart.above(5);
                    if (this.makeLimb(level, trunkSetter, random, checkStart, checkEnd, false, config)) {
                        int dx = origin.getX() - checkStart.getX();
                        int dz = origin.getZ() - checkStart.getZ();
                        double branchHeight = checkStart.getY() - Math.sqrt(dx * dx + dz * dz) * 0.381;
                        int branchTop = branchHeight > trunkTop ? trunkTop : (int)branchHeight;
                        BlockPos checkBranchBase = new BlockPos(origin.getX(), branchTop, origin.getZ());
                        if (this.makeLimb(level, trunkSetter, random, checkBranchBase, checkStart, false, config)) {
                            foliageCoords.add(new FancyTrunkPlacer.FoliageCoords(checkStart, checkBranchBase.getY()));
                        }
                    }
                }
            }
        }

        this.makeLimb(level, trunkSetter, random, origin, origin.above(trunkHeight), true, config);
        this.makeBranches(level, trunkSetter, random, height, origin, foliageCoords, config);
        List<FoliagePlacer.FoliageAttachment> attachments = Lists.newArrayList();

        for (FancyTrunkPlacer.FoliageCoords foliageCoord : foliageCoords) {
            if (this.trimBranches(height, foliageCoord.getBranchBase() - origin.getY())) {
                attachments.add(foliageCoord.attachment);
            }
        }

        return attachments;
    }

    private boolean makeLimb(
        final WorldGenLevel level,
        final BiConsumer<BlockPos, BlockState> trunkSetter,
        final RandomSource random,
        final BlockPos startPos,
        final BlockPos endPos,
        final boolean doPlace,
        final TreeConfiguration config
    ) {
        if (!doPlace && Objects.equals(startPos, endPos)) {
            return true;
        }

        BlockPos delta = endPos.offset(-startPos.getX(), -startPos.getY(), -startPos.getZ());
        int steps = this.getSteps(delta);
        float dx = (float)delta.getX() / steps;
        float dy = (float)delta.getY() / steps;
        float dz = (float)delta.getZ() / steps;

        for (int i = 0; i <= steps; i++) {
            BlockPos blockPos = startPos.offset(Mth.floor(0.5F + i * dx), Mth.floor(0.5F + i * dy), Mth.floor(0.5F + i * dz));
            if (doPlace) {
                this.placeLog(
                    level, trunkSetter, random, blockPos, config, state -> state.trySetValue(RotatedPillarBlock.AXIS, this.getLogAxis(startPos, blockPos))
                );
            } else if (!this.isFree(level, blockPos)) {
                return false;
            }
        }

        return true;
    }

    private int getSteps(final BlockPos pos) {
        int absX = Mth.abs(pos.getX());
        int absY = Mth.abs(pos.getY());
        int absZ = Mth.abs(pos.getZ());
        return Math.max(absX, Math.max(absY, absZ));
    }

    private Direction.Axis getLogAxis(final BlockPos startPos, final BlockPos blockPos) {
        Direction.Axis axis = Direction.Axis.Y;
        int xdiff = Math.abs(blockPos.getX() - startPos.getX());
        int zdiff = Math.abs(blockPos.getZ() - startPos.getZ());
        int maxdiff = Math.max(xdiff, zdiff);
        if (maxdiff > 0) {
            if (xdiff == maxdiff) {
                axis = Direction.Axis.X;
            } else {
                axis = Direction.Axis.Z;
            }
        }

        return axis;
    }

    private boolean trimBranches(final int height, final int localY) {
        return localY >= height * 0.2;
    }

    private void makeBranches(
        final WorldGenLevel level,
        final BiConsumer<BlockPos, BlockState> trunkSetter,
        final RandomSource random,
        final int height,
        final BlockPos origin,
        final List<FancyTrunkPlacer.FoliageCoords> foliageCoords,
        final TreeConfiguration config
    ) {
        for (FancyTrunkPlacer.FoliageCoords endCoord : foliageCoords) {
            int branchBase = endCoord.getBranchBase();
            BlockPos baseCoord = new BlockPos(origin.getX(), branchBase, origin.getZ());
            if (!baseCoord.equals(endCoord.attachment.pos()) && this.trimBranches(height, branchBase - origin.getY())) {
                this.makeLimb(level, trunkSetter, random, baseCoord, endCoord.attachment.pos(), true, config);
            }
        }
    }

    private static float treeShape(final int height, final int y) {
        if (y < height * 0.3F) {
            return -1.0F;
        }

        float radius = height / 2.0F;
        float adjacent = radius - y;
        float distance = Mth.sqrt(radius * radius - adjacent * adjacent);
        if (adjacent == 0.0F) {
            distance = radius;
        } else if (Math.abs(adjacent) >= radius) {
            return 0.0F;
        }

        return distance * 0.5F;
    }

    private static class FoliageCoords {
        private final FoliagePlacer.FoliageAttachment attachment;
        private final int branchBase;

        public FoliageCoords(final BlockPos pos, final int branchBase) {
            this.attachment = new FoliagePlacer.FoliageAttachment(pos, 0, false);
            this.branchBase = branchBase;
        }

        public int getBranchBase() {
            return this.branchBase;
        }
    }
}