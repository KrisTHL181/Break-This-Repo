package net.minecraft.util;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnUtil {
   public static <T extends Mob> Optional<T> trySpawnMob(
      final EntityType<T> entityType,
      final EntitySpawnReason spawnReason,
      final ServerLevel level,
      final BlockPos start,
      final int spawnAttempts,
      final int spawnRangeXZ,
      final int spawnRangeY,
      final SpawnUtil.Strategy strategy,
      final boolean checkCollisions
   ) {
      BlockPos.MutableBlockPos searchPos = start.mutable();
      RandomSource random = level.getRandom();

      for (int i = 0; i < spawnAttempts; i++) {
         int dx = Mth.randomBetweenInclusive(random, -spawnRangeXZ, spawnRangeXZ);
         int dz = Mth.randomBetweenInclusive(random, -spawnRangeXZ, spawnRangeXZ);
         searchPos.setWithOffset(start, dx, spawnRangeY, dz);
         if (level.getWorldBorder().isWithinBounds(searchPos)
            && moveToPossibleSpawnPosition(level, spawnRangeY, searchPos, strategy)
            && (!checkCollisions || level.noCollision(entityType.getSpawnAABB(searchPos.getX() + 0.5, searchPos.getY(), searchPos.getZ() + 0.5)))) {
            T mob = (T)entityType.create(level, null, searchPos, spawnReason, false, false);
            if (mob != null) {
               if (mob.checkSpawnRules(level, spawnReason) && mob.checkSpawnObstruction(level)) {
                  level.addFreshEntityWithPassengers(mob);
                  mob.playAmbientSound();
                  return Optional.of(mob);
               }

               mob.discard();
            }
         }
      }

      return Optional.empty();
   }

   private static boolean moveToPossibleSpawnPosition(
      final ServerLevel level, final int spawnRangeY, final BlockPos.MutableBlockPos searchPos, final SpawnUtil.Strategy strategy
   ) {
      BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos().set(searchPos);
      BlockState aboveState = level.getBlockState(abovePos);

      for (int y = spawnRangeY; y >= -spawnRangeY; y--) {
         searchPos.move(Direction.DOWN);
         abovePos.setWithOffset(searchPos, Direction.UP);
         BlockState currentState = level.getBlockState(searchPos);
         if (strategy.canSpawnOn(level, searchPos, currentState, abovePos, aboveState)) {
            searchPos.move(Direction.UP);
            return true;
         }

         aboveState = currentState;
      }

      return false;
   }

   public interface Strategy {
      @Deprecated
      SpawnUtil.Strategy LEGACY_IRON_GOLEM = (level, pos, blockState, abovePos, aboveState) -> !blockState.is(Blocks.COBWEB)
            && !blockState.is(Blocks.CACTUS)
            && !blockState.is(Blocks.GLASS_PANE)
            && !(blockState.getBlock() instanceof StainedGlassPaneBlock)
            && !(blockState.getBlock() instanceof StainedGlassBlock)
            && !(blockState.getBlock() instanceof LeavesBlock)
            && !blockState.is(Blocks.CONDUIT)
            && !blockState.is(Blocks.ICE)
            && !blockState.is(Blocks.TNT)
            && !blockState.is(Blocks.GLOWSTONE)
            && !blockState.is(Blocks.BEACON)
            && !blockState.is(Blocks.SEA_LANTERN)
            && !blockState.is(Blocks.FROSTED_ICE)
            && !blockState.is(Blocks.TINTED_GLASS)
            && !blockState.is(Blocks.GLASS)
         ? (aboveState.isAir() || aboveState.liquid()) && (blockState.isSolid() || blockState.is(Blocks.POWDER_SNOW))
         : false;
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (level, pos, blockState, abovePos, aboveState) -> aboveState.getCollisionShape(level, abovePos).isEmpty()
         && Block.isFaceFull(blockState.getCollisionShape(level, pos), Direction.UP);
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER_NO_LEAVES = (level, pos, blockState, abovePos, aboveState) -> aboveState.getCollisionShape(level, abovePos)
            .isEmpty()
         && !blockState.is(BlockTags.LEAVES)
         && Block.isFaceFull(blockState.getCollisionShape(level, pos), Direction.UP);

      boolean canSpawnOn(ServerLevel level, BlockPos pos, BlockState blockState, BlockPos abovePos, BlockState aboveState);
   }
}
