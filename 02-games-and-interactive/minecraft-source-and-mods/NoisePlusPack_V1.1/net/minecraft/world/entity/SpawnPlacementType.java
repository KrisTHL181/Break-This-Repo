package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

public interface SpawnPlacementType {
   boolean isSpawnPositionOk(LevelReader var1, BlockPos var2, @Nullable EntityType<?> var3);

   default BlockPos adjustSpawnPosition(LevelReader p_331949_, BlockPos p_333622_) {
      return p_333622_;
   }
}
