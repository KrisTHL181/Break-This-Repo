package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
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

   public RemoveBlockGoal(final Block blockToRemove, final PathfinderMob mob, final double speedModifier, final int verticalSearchRange) {
      super(mob, speedModifier, 24, verticalSearchRange);
      this.blockToRemove = blockToRemove;
      this.removerMob = mob;
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

   public void playDestroyProgressSound(final LevelAccessor level, final BlockPos pos) {
   }

   public void playBreakSound(final Level level, final BlockPos pos) {
   }

   @Override
   public void tick() {
      super.tick();
      Level level = this.removerMob.level();
      BlockPos mobPos = this.removerMob.blockPosition();
      BlockPos eatPos = this.getPosWithBlock(mobPos, level);
      RandomSource random = this.removerMob.getRandom();
      if (this.isReachedTarget() && eatPos != null) {
         if (this.ticksSinceReachedGoal > 0) {
            Vec3 movement = this.removerMob.getDeltaMovement();
            this.removerMob.setDeltaMovement(movement.x, 0.3, movement.z);
            if (level instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(
                  new ItemParticleOption(ParticleTypes.ITEM, Items.EGG),
                  eatPos.getX() + 0.5,
                  eatPos.getY() + 0.7,
                  eatPos.getZ() + 0.5,
                  3,
                  (random.nextFloat() - 0.5) * 0.08,
                  (random.nextFloat() - 0.5) * 0.08,
                  (random.nextFloat() - 0.5) * 0.08,
                  0.15F
               );
            }
         }

         if (this.ticksSinceReachedGoal % 2 == 0) {
            Vec3 movement = this.removerMob.getDeltaMovement();
            this.removerMob.setDeltaMovement(movement.x, -0.3, movement.z);
            if (this.ticksSinceReachedGoal % 6 == 0) {
               this.playDestroyProgressSound(level, this.blockPos);
            }
         }

         if (this.ticksSinceReachedGoal > 60) {
            level.removeBlock(eatPos, false);
            if (!level.isClientSide()) {
               for (int i = 0; i < 20; i++) {
                  double xa = random.nextGaussian() * 0.02;
                  double ya = random.nextGaussian() * 0.02;
                  double za = random.nextGaussian() * 0.02;
                  ((ServerLevel)level).sendParticles(ParticleTypes.POOF, eatPos.getX() + 0.5, eatPos.getY(), eatPos.getZ() + 0.5, 1, xa, ya, za, 0.15F);
               }

               this.playBreakSound(level, eatPos);
            }
         }

         this.ticksSinceReachedGoal++;
      }
   }

   private @Nullable BlockPos getPosWithBlock(final BlockPos pos, final BlockGetter level) {
      if (level.getBlockState(pos).is(this.blockToRemove)) {
         return pos;
      }

      BlockPos[] neighbours = new BlockPos[]{pos.below(), pos.west(), pos.east(), pos.north(), pos.south(), pos.below().below()};

      for (BlockPos neighborPos : neighbours) {
         if (level.getBlockState(neighborPos).is(this.blockToRemove)) {
            return neighborPos;
         }
      }

      return null;
   }

   @Override
   protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
      ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
      return chunk == null
         ? false
         : chunk.getBlockState(pos).is(this.blockToRemove) && chunk.getBlockState(pos.above()).isAir() && chunk.getBlockState(pos.above(2)).isAir();
   }
}
