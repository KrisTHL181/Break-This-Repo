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
      EntityType<T> p_216404_,
      EntitySpawnReason p_369127_,
      ServerLevel p_216406_,
      BlockPos p_216407_,
      int p_216408_,
      int p_216409_,
      int p_216410_,
      SpawnUtil.Strategy p_216411_,
      boolean p_378332_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_216407_.mutable();

      for (int i = 0; i < p_216408_; i++) {
         int j = Mth.randomBetweenInclusive(p_216406_.random, -p_216409_, p_216409_);
         int k = Mth.randomBetweenInclusive(p_216406_.random, -p_216409_, p_216409_);
         blockpos$mutableblockpos.setWithOffset(p_216407_, j, p_216410_, k);
         if (p_216406_.getWorldBorder().isWithinBounds(blockpos$mutableblockpos)
            && moveToPossibleSpawnPosition(p_216406_, p_216410_, blockpos$mutableblockpos, p_216411_)
            && (
               !p_378332_
                  || p_216406_.noCollision(
                     p_216404_.getSpawnAABB(blockpos$mutableblockpos.getX() + 0.5, blockpos$mutableblockpos.getY(), blockpos$mutableblockpos.getZ() + 0.5)
                  )
            )) {
            T t = (T)p_216404_.create(p_216406_, null, blockpos$mutableblockpos, p_369127_, false, false);
            if (t != null) {
               if (t.checkSpawnRules(p_216406_, p_369127_) && t.checkSpawnObstruction(p_216406_)) {
                  p_216406_.addFreshEntityWithPassengers(t);
                  t.playAmbientSound();
                  return Optional.of(t);
               }

               t.discard();
            }
         }
      }

      return Optional.empty();
   }

   private static boolean moveToPossibleSpawnPosition(ServerLevel p_216399_, int p_216400_, BlockPos.MutableBlockPos p_216401_, SpawnUtil.Strategy p_216402_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos().set(p_216401_);
      BlockState blockstate = p_216399_.getBlockState(blockpos$mutableblockpos);

      for (int i = p_216400_; i >= -p_216400_; i--) {
         p_216401_.move(Direction.DOWN);
         blockpos$mutableblockpos.setWithOffset(p_216401_, Direction.UP);
         BlockState blockstate1 = p_216399_.getBlockState(p_216401_);
         if (p_216402_.canSpawnOn(p_216399_, p_216401_, blockstate1, blockpos$mutableblockpos, blockstate)) {
            p_216401_.move(Direction.UP);
            return true;
         }

         blockstate = blockstate1;
      }

      return false;
   }

   public interface Strategy {
      @Deprecated
      SpawnUtil.Strategy LEGACY_IRON_GOLEM = (p_289751_, p_289752_, p_289753_, p_289754_, p_289755_) -> !p_289753_.is(Blocks.COBWEB)
            && !p_289753_.is(Blocks.CACTUS)
            && !p_289753_.is(Blocks.GLASS_PANE)
            && !(p_289753_.getBlock() instanceof StainedGlassPaneBlock)
            && !(p_289753_.getBlock() instanceof StainedGlassBlock)
            && !(p_289753_.getBlock() instanceof LeavesBlock)
            && !p_289753_.is(Blocks.CONDUIT)
            && !p_289753_.is(Blocks.ICE)
            && !p_289753_.is(Blocks.TNT)
            && !p_289753_.is(Blocks.GLOWSTONE)
            && !p_289753_.is(Blocks.BEACON)
            && !p_289753_.is(Blocks.SEA_LANTERN)
            && !p_289753_.is(Blocks.FROSTED_ICE)
            && !p_289753_.is(Blocks.TINTED_GLASS)
            && !p_289753_.is(Blocks.GLASS)
         ? (p_289755_.isAir() || p_289755_.liquid()) && (p_289753_.isSolid() || p_289753_.is(Blocks.POWDER_SNOW))
         : false;
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (p_358812_, p_358813_, p_358814_, p_358815_, p_358816_) -> p_358816_.getCollisionShape(p_358812_, p_358815_)
            .isEmpty()
         && Block.isFaceFull(p_358814_.getCollisionShape(p_358812_, p_358813_), Direction.UP);
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER_NO_LEAVES = (p_358807_, p_358808_, p_358809_, p_358810_, p_358811_) -> p_358811_.getCollisionShape(
               p_358807_, p_358810_
            )
            .isEmpty()
         && !p_358809_.is(BlockTags.LEAVES)
         && Block.isFaceFull(p_358809_.getCollisionShape(p_358807_, p_358808_), Direction.UP);

      boolean canSpawnOn(ServerLevel var1, BlockPos var2, BlockState var3, BlockPos var4, BlockState var5);
   }
}
