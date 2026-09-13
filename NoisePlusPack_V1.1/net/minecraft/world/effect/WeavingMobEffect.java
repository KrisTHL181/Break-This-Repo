package net.minecraft.world.effect;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

class WeavingMobEffect extends MobEffect {
   private final ToIntFunction<RandomSource> maxCobwebs;

   protected WeavingMobEffect(MobEffectCategory p_331231_, int p_336179_, ToIntFunction<RandomSource> p_328620_) {
      super(p_331231_, p_336179_, ParticleTypes.ITEM_COBWEB);
      this.maxCobwebs = p_328620_;
   }

   @Override
   public void onMobRemoved(ServerLevel p_362050_, LivingEntity p_335117_, int p_333338_, Entity.RemovalReason p_328096_) {
      if (p_328096_ == Entity.RemovalReason.KILLED && (p_335117_ instanceof Player || p_362050_.getGameRules().get(GameRules.MOB_GRIEFING))) {
         this.spawnCobwebsRandomlyAround(p_362050_, p_335117_.getRandom(), p_335117_.blockPosition());
      }
   }

   private void spawnCobwebsRandomlyAround(ServerLevel p_368804_, RandomSource p_332035_, BlockPos p_329542_) {
      Set<BlockPos> set = Sets.newHashSet();
      int i = this.maxCobwebs.applyAsInt(p_332035_);

      for (BlockPos blockpos : BlockPos.randomInCube(p_332035_, 15, p_329542_, 1)) {
         BlockPos blockpos1 = blockpos.below();
         if (!set.contains(blockpos)
            && p_368804_.getBlockState(blockpos).canBeReplaced()
            && p_368804_.getBlockState(blockpos1).isFaceSturdy(p_368804_, blockpos1, Direction.UP)) {
            set.add(blockpos.immutable());
            if (set.size() >= i) {
               break;
            }
         }
      }

      for (BlockPos blockpos2 : set) {
         p_368804_.setBlock(blockpos2, Blocks.COBWEB.defaultBlockState(), 3);
         p_368804_.levelEvent(3018, blockpos2, 0);
      }
   }
}
