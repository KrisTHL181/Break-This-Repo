package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RemoveBlockGoal extends MoveToBlockGoal {
   private final Block blockToRemove;
   private final Mob removerMob;
   private int ticksSinceReachedGoal;
   private static final int WAIT_AFTER_BLOCK_FOUND = 20;

   public RemoveBlockGoal(Block p_25840_, PathfinderMob p_25841_, double p_25842_, int p_25843_) {
      super(p_25841_, p_25842_, 24, p_25843_);
      this.blockToRemove = p_25840_;
      this.removerMob = p_25841_;
   }

   @Override
   public boolean canUse() {
      if (!getServerLevel(this.removerMob).getGameRules().get(GameRules.MOB_GRIEFING)) {
         return false;
      } else if (this.nextStartTick > 0) {
         this.nextStartTick--;
         return false;
      } else if (this.findNearestBlock()) {
         this.nextStartTick = reducedTickDelay(20);
         return true;
      } else {
         this.nextStartTick = this.nextStartTick(this.mob);
         return false;
      }
   }

   @Override
   public void stop() {
      super.stop();
      this.removerMob.fallDistance = 1.0;
   }

   @Override
   public void start() {
      super.start();
      this.ticksSinceReachedGoal = 0;
   }

   public void playDestroyProgressSound(LevelAccessor p_25847_, BlockPos p_25848_) {
   }

   public void playBreakSound(Level p_25845_, BlockPos p_25846_) {
   }

   @Override
   public void tick() {
      super.tick();
      Level level = this.removerMob.level();
      BlockPos blockpos = this.removerMob.blockPosition();
      BlockPos blockpos1 = this.getPosWithBlock(blockpos, level);
      RandomSource randomsource = this.removerMob.getRandom();
      if (this.isReachedTarget() && blockpos1 != null) {
         if (this.ticksSinceReachedGoal > 0) {
            Vec3 vec3 = this.removerMob.getDeltaMovement();
            this.removerMob.setDeltaMovement(vec3.x, 0.3, vec3.z);
            if (!level.isClientSide()) {
               double d0 = 0.08;
               ((ServerLevel)level)
                  .sendParticles(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EGG)),
                     blockpos1.getX() + 0.5,
                     blockpos1.getY() + 0.7,
                     blockpos1.getZ() + 0.5,
                     3,
                     (randomsource.nextFloat() - 0.5) * 0.08,
                     (randomsource.nextFloat() - 0.5) * 0.08,
                     (randomsource.nextFloat() - 0.5) * 0.08,
                     0.15F
                  );
            }
         }

         if (this.ticksSinceReachedGoal % 2 == 0) {
            Vec3 vec31 = this.removerMob.getDeltaMovement();
            this.removerMob.setDeltaMovement(vec31.x, -0.3, vec31.z);
            if (this.ticksSinceReachedGoal % 6 == 0) {
               this.playDestroyProgressSound(level, this.blockPos);
            }
         }

         if (this.ticksSinceReachedGoal > 60) {
            level.removeBlock(blockpos1, false);
            if (!level.isClientSide()) {
               for (int i = 0; i < 20; i++) {
                  double d3 = randomsource.nextGaussian() * 0.02;
                  double d1 = randomsource.nextGaussian() * 0.02;
                  double d2 = randomsource.nextGaussian() * 0.02;
                  ((ServerLevel)level)
                     .sendParticles(ParticleTypes.POOF, blockpos1.getX() + 0.5, blockpos1.getY(), blockpos1.getZ() + 0.5, 1, d3, d1, d2, 0.15F);
               }

               this.playBreakSound(level, blockpos1);
            }
         }

         this.ticksSinceReachedGoal++;
      }
   }

   private @Nullable BlockPos getPosWithBlock(BlockPos p_25853_, BlockGetter p_25854_) {
      if (p_25854_.getBlockState(p_25853_).is(this.blockToRemove)) {
         return p_25853_;
      }

      BlockPos[] ablockpos = new BlockPos[]{p_25853_.below(), p_25853_.west(), p_25853_.east(), p_25853_.north(), p_25853_.south(), p_25853_.below().below()};

      for (BlockPos blockpos : ablockpos) {
         if (p_25854_.getBlockState(blockpos).is(this.blockToRemove)) {
            return blockpos;
         }
      }

      return null;
   }

   @Override
   protected boolean isValidTarget(LevelReader p_25850_, BlockPos p_25851_) {
      ChunkAccess chunkaccess = p_25850_.getChunk(
         SectionPos.blockToSectionCoord(p_25851_.getX()), SectionPos.blockToSectionCoord(p_25851_.getZ()), ChunkStatus.FULL, false
      );
      return chunkaccess == null
         ? false
         : chunkaccess.getBlockState(p_25851_).is(this.blockToRemove)
            && chunkaccess.getBlockState(p_25851_.above()).isAir()
            && chunkaccess.getBlockState(p_25851_.above(2)).isAir();
   }
}
