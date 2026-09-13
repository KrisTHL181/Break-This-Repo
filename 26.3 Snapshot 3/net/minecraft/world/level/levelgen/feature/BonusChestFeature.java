package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public record BonusChestFeature() implements Feature {
   public static final MapCodec<BonusChestFeature> CODEC = MapCodec.unit(BonusChestFeature::new);

   @Override
   public MapCodec<BonusChestFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      ChunkPos chunkPos = ChunkPos.containing(origin);
      IntArrayList xPoses = Util.toShuffledList(IntStream.rangeClosed(chunkPos.getMinBlockX(), chunkPos.getMaxBlockX()), random);
      IntArrayList zPoses = Util.toShuffledList(IntStream.rangeClosed(chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ()), random);
      BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
      IntListIterator var9 = xPoses.iterator();

      while (var9.hasNext()) {
         Integer x = (Integer)var9.next();
         IntListIterator var11 = zPoses.iterator();

         while (var11.hasNext()) {
            Integer z = (Integer)var11.next();
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
