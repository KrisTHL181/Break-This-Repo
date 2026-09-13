package net.minecraft.world.level.block.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class AmbientDesertBlockSoundsPlayer {
   private static final int IDLE_SOUND_CHANCE = 2100;
   private static final int DRY_GRASS_SOUND_CHANCE = 200;
   private static final int DEAD_BUSH_SOUND_CHANCE = 130;
   private static final int DEAD_BUSH_SOUND_BADLANDS_DECREASED_CHANCE = 3;
   private static final int SURROUNDING_BLOCKS_PLAY_SOUND_THRESHOLD = 3;
   private static final int SURROUNDING_BLOCKS_DISTANCE_HORIZONTAL_CHECK = 8;
   private static final int SURROUNDING_BLOCKS_DISTANCE_VERTICAL_CHECK = 5;
   private static final int HORIZONTAL_DIRECTIONS = 4;

   public static void playAmbientSandSounds(final Level level, final BlockPos pos, final RandomSource random) {
      if (level.getBlockState(pos.above()).is(Blocks.AIR)) {
         if (random.nextInt(2100) == 0 && shouldPlayAmbientSandSound(level, pos)) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.SAND_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }
      }
   }

   public static void playAmbientDryGrassSounds(final Level level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(200) == 0 && shouldPlayDesertDryVegetationBlockSounds(level, pos.below())) {
         level.playPlayerSound(SoundEvents.DRY_GRASS, SoundSource.AMBIENT, 1.0F, 1.0F);
      }
   }

   public static void playAmbientDeadBushSounds(final Level level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(130) == 0) {
         BlockState belowPos = level.getBlockState(pos.below());
         if ((belowPos.is(Blocks.RED_SAND) || belowPos.is(BlockTags.TERRACOTTA)) && random.nextInt(3) != 0) {
            return;
         }

         if (shouldPlayDesertDryVegetationBlockSounds(level, pos.below())) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.DEAD_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }
      }
   }

   public static boolean shouldPlayDesertDryVegetationBlockSounds(final Level level, final BlockPos belowPos) {
      return level.getBlockState(belowPos).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS)
         && level.getBlockState(belowPos.below()).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS);
   }

   private static boolean shouldPlayAmbientSandSound(final Level level, final BlockPos pos) {
      int matchingBlocksFound = 0;
      int sidesChecked = 0;
      BlockPos.MutableBlockPos mutablePos = pos.mutable();

      for (Direction dir : Direction.Plane.HORIZONTAL) {
         mutablePos.set(pos).move(dir, 8);
         if (columnContainsTriggeringBlock(level, mutablePos) && matchingBlocksFound++ >= 3) {
            return true;
         }

         sidesChecked++;
         int remainingSides = 4 - sidesChecked;
         int potentialMatches = remainingSides + matchingBlocksFound;
         boolean canStillFindRequiredSoundTriggerBlocks = potentialMatches >= 3;
         if (!canStillFindRequiredSoundTriggerBlocks) {
            return false;
         }
      }

      return false;
   }

   private static boolean columnContainsTriggeringBlock(final Level level, final BlockPos.MutableBlockPos mutablePos) {
      int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, mutablePos) - 1;
      if (Math.abs(surfaceY - mutablePos.getY()) > 5) {
         mutablePos.move(Direction.UP, 6);
         BlockState aboveBlockState = level.getBlockState(mutablePos);
         mutablePos.move(Direction.DOWN);

         for (int i = 0; i < 10; i++) {
            BlockState currentBlockState = level.getBlockState(mutablePos);
            if (aboveBlockState.isAir() && canTriggerAmbientDesertSandSounds(currentBlockState)) {
               return true;
            }

            aboveBlockState = currentBlockState;
            mutablePos.move(Direction.DOWN);
         }

         return false;
      } else {
         boolean hasAirAbove = level.getBlockState(mutablePos.setY(surfaceY + 1)).isAir();
         return hasAirAbove && canTriggerAmbientDesertSandSounds(level.getBlockState(mutablePos.setY(surfaceY)));
      }
   }

   private static boolean canTriggerAmbientDesertSandSounds(final BlockState blockState) {
      return blockState.is(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS);
   }
}
