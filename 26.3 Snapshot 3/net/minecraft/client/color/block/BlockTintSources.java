package net.minecraft.client.color.block;

import java.util.Set;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockTintSources {
   public static BlockTintSource constant(final int color) {
      return var1 -> color;
   }

   public static BlockTintSource constant(final int colorInHand, final int colorInWorld) {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return colorInHand;
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return colorInWorld;
         }
      };
   }

   public static BlockTintSource doubleTallGrass() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return GrassColor.getDefaultColor();
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageGrassColor(level, state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos);
         }

         @Override
         public Set<Property<?>> relevantProperties() {
            return Set.of(DoublePlantBlock.HALF);
         }
      };
   }

   public static BlockTintSource grass() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return GrassColor.getDefaultColor();
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageGrassColor(level, pos);
         }
      };
   }

   public static BlockTintSource grassBlock() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return GrassColor.getDefaultColor();
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageGrassColor(level, pos);
         }

         @Override
         public int colorAsTerrainParticle(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return -1;
         }
      };
   }

   public static BlockTintSource sugarCane() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return -1;
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageGrassColor(level, pos);
         }
      };
   }

   public static BlockTintSource foliage() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return -12012264;
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageFoliageColor(level, pos);
         }
      };
   }

   public static BlockTintSource dryFoliage() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return -10732494;
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageDryFoliageColor(level, pos);
         }
      };
   }

   public static BlockTintSource water() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return -1;
         }

         @Override
         public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageWaterColor(level, pos);
         }
      };
   }

   public static BlockTintSource waterParticles() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return -1;
         }

         @Override
         public int colorAsTerrainParticle(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
            return BiomeColors.getAverageWaterColor(level, pos);
         }
      };
   }

   public static BlockTintSource redstone() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            return RedStoneWireBlock.getColorForPower(state.getValue(RedStoneWireBlock.POWER));
         }

         @Override
         public Set<Property<?>> relevantProperties() {
            return Set.of(RedStoneWireBlock.POWER);
         }
      };
   }

   public static BlockTintSource stem() {
      return new BlockTintSource() {
         @Override
         public int color(final BlockState state) {
            int age = state.getValue(StemBlock.AGE);
            return ARGB.color(age * 32, 255 - age * 8, age * 4);
         }

         @Override
         public Set<Property<?>> relevantProperties() {
            return Set.of(StemBlock.AGE);
         }
      };
   }
}
