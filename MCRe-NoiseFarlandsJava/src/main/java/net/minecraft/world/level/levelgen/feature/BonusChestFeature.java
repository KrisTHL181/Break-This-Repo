package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BonusChestFeature extends Feature<NoneFeatureConfiguration> {
    public BonusChestFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = ChunkPos.containing(context.origin());
        List<Integer> xPoses = Util.toShuffledList(IntStream.rangeClosed((int)chunkPos.getMinBlockX(), (int)chunkPos.getMaxBlockX()), random);
        List<Integer> zPoses = Util.toShuffledList(IntStream.rangeClosed((int)chunkPos.getMinBlockZ(), (int)chunkPos.getMaxBlockZ()), random);
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();

        for (Integer x : xPoses) {
            for (Integer z : zPoses) {
                mutPos.set(x, 0, z);
                BlockPos chestPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutPos);
                if (level.isEmptyBlock(chestPos) || level.getBlockState(chestPos).getCollisionShape(level, chestPos).isEmpty()) {
                    level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
                    RandomizableContainer.setBlockEntityLootTable(level, random, chestPos, BuiltInLootTables.SPAWN_BONUS_CHEST);
                    BlockState torch = Blocks.TORCH.defaultBlockState();

                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos torchPos = chestPos.relative(direction);
                        if (torch.canSurvive(level, torchPos)) {
                            level.setBlock(torchPos, torch, 2);
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }
}