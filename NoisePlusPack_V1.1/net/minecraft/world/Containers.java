package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Containers {
   public static void dropContents(Level p_19003_, BlockPos p_19004_, Container p_19005_) {
      dropContents(p_19003_, p_19004_.getX(), p_19004_.getY(), p_19004_.getZ(), p_19005_);
   }

   public static void dropContents(Level p_18999_, Entity p_19000_, Container p_19001_) {
      dropContents(p_18999_, p_19000_.getX(), p_19000_.getY(), p_19000_.getZ(), p_19001_);
   }

   private static void dropContents(Level p_18987_, double p_18988_, double p_18989_, double p_18990_, Container p_18991_) {
      for (int i = 0; i < p_18991_.getContainerSize(); i++) {
         dropItemStack(p_18987_, p_18988_, p_18989_, p_18990_, p_18991_.getItem(i));
      }
   }

   public static void dropContents(Level p_19011_, BlockPos p_19012_, NonNullList<ItemStack> p_19013_) {
      p_19013_.forEach(p_19009_ -> dropItemStack(p_19011_, p_19012_.getX(), p_19012_.getY(), p_19012_.getZ(), p_19009_));
   }

   public static void dropItemStack(Level p_18993_, double p_18994_, double p_18995_, double p_18996_, ItemStack p_18997_) {
      double d0 = EntityType.ITEM.getWidth();
      double d1 = 1.0 - d0;
      double d2 = d0 / 2.0;
      double d3 = Math.floor(p_18994_) + p_18993_.random.nextDouble() * d1 + d2;
      double d4 = Math.floor(p_18995_) + p_18993_.random.nextDouble() * d1;
      double d5 = Math.floor(p_18996_) + p_18993_.random.nextDouble() * d1 + d2;

      while (!p_18997_.isEmpty()) {
         ItemEntity itementity = new ItemEntity(p_18993_, d3, d4, d5, p_18997_.split(p_18993_.random.nextInt(21) + 10));
         float f = 0.05F;
         itementity.setDeltaMovement(
            p_18993_.random.triangle(0.0, 0.11485000171139836),
            p_18993_.random.triangle(0.2, 0.11485000171139836),
            p_18993_.random.triangle(0.0, 0.11485000171139836)
         );
         p_18993_.addFreshEntity(itementity);
      }
   }

   public static void updateNeighboursAfterDestroy(BlockState p_394534_, Level p_395234_, BlockPos p_397575_) {
      p_395234_.updateNeighbourForOutputSignal(p_397575_, p_394534_.getBlock());
   }
}
