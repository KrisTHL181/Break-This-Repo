package net.minecraft.world.level;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface CommonLevelAccessor extends LevelReader, LevelSimulatedRW, EntityGetter {
   @Override
   default <T extends BlockEntity> Optional<T> getBlockEntity(final BlockPos pos, final BlockEntityType<T> type) {
      return LevelReader.super.getBlockEntity(pos, type);
   }

   @Override
   default List<VoxelShape> getEntityCollisions(final @Nullable Entity source, final AABB testArea) {
      return EntityGetter.super.getEntityCollisions(source, testArea);
   }

   @Override
   default boolean isUnobstructed(final @Nullable Entity source, final VoxelShape shape) {
      return EntityGetter.super.isUnobstructed(source, shape);
   }

   @Override
   default BlockPos getHeightmapPos(final Heightmap.Types type, final BlockPos pos) {
      return LevelReader.super.getHeightmapPos(type, pos);
   }
}
