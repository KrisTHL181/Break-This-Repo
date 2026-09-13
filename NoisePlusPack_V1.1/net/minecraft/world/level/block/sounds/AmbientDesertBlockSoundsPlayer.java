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

   public static void playAmbientSandSounds(Level p_410569_, BlockPos p_406493_, RandomSource p_406623_) {
      if (p_410569_.getBlockState(p_406493_.above()).is(Blocks.AIR) && p_406623_.nextInt(2100) == 0 && shouldPlayAmbientSandSound(p_410569_, p_406493_)) {
         p_410569_.playLocalSound(p_406493_.getX(), p_406493_.getY(), p_406493_.getZ(), SoundEvents.SAND_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
      }
   }

   public static void playAmbientDryGrassSounds(Level p_408666_, BlockPos p_410105_, RandomSource p_408420_) {
      if (p_408420_.nextInt(200) == 0 && shouldPlayDesertDryVegetationBlockSounds(p_408666_, p_410105_.below())) {
         p_408666_.playPlayerSound(SoundEvents.DRY_GRASS, SoundSource.AMBIENT, 1.0F, 1.0F);
      }
   }

   public static void playAmbientDeadBushSounds(Level p_410214_, BlockPos p_407176_, RandomSource p_409168_) {
      if (p_409168_.nextInt(130) == 0) {
         BlockState blockstate = p_410214_.getBlockState(p_407176_.below());
         if ((blockstate.is(Blocks.RED_SAND) || blockstate.is(BlockTags.TERRACOTTA)) && p_409168_.nextInt(3) != 0) {
            return;
         }

         if (shouldPlayDesertDryVegetationBlockSounds(p_410214_, p_407176_.below())) {
            p_410214_.playLocalSound(p_407176_.getX(), p_407176_.getY(), p_407176_.getZ(), SoundEvents.DEAD_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }
      }
   }

   public static boolean shouldPlayDesertDryVegetationBlockSounds(Level p_410659_, BlockPos p_408797_) {
      return p_410659_.getBlockState(p_408797_).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS)
         && p_410659_.getBlockState(p_408797_.below()).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS);
   }

   private static boolean shouldPlayAmbientSandSound(Level p_405903_, BlockPos p_408393_) {
      int i = 0;
      int j = 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_408393_.mutable();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         blockpos$mutableblockpos.set(p_408393_).move(direction, 8);
         if (columnContainsTriggeringBlock(p_405903_, blockpos$mutableblockpos) && i++ >= 3) {
            return true;
         }

         j++;
         int k = 4 - j;
         int l = k + i;
         boolean flag = l >= 3;
         if (!flag) {
            return false;
         }
      }

      return false;
   }

   private static boolean columnContainsTriggeringBlock(Level p_410151_, BlockPos.MutableBlockPos p_409811_) {
      int i = p_410151_.getHeight(Heightmap.Types.WORLD_SURFACE, p_409811_) - 1;
      if (Math.abs(i - p_409811_.getY()) > 5) {
         p_409811_.move(Direction.UP, 6);
         BlockState blockstate1 = p_410151_.getBlockState(p_409811_);
         p_409811_.move(Direction.DOWN);

         for (int j = 0; j < 10; j++) {
            BlockState blockstate = p_410151_.getBlockState(p_409811_);
            if (blockstate1.isAir() && canTriggerAmbientDesertSandSounds(blockstate)) {
               return true;
            }

            blockstate1 = blockstate;
            p_409811_.move(Direction.DOWN);
         }

         return false;
      } else {
         boolean flag = p_410151_.getBlockState(p_409811_.setY(i + 1)).isAir();
         return flag && canTriggerAmbientDesertSandSounds(p_410151_.getBlockState(p_409811_.setY(i)));
      }
   }

   private static boolean canTriggerAmbientDesertSandSounds(BlockState p_407526_) {
      return p_407526_.is(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS);
   }
}
