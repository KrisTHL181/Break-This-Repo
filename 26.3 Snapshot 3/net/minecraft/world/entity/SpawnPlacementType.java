package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

public interface SpawnPlacementType {
   boolean isSpawnPositionOk(LevelReader level, BlockPos blockPos, @Nullable EntityType<?> type);

   default BlockPos adjustSpawnPosition(final LevelReader level, final BlockPos candidate) {
      return candidate;
   }
}
