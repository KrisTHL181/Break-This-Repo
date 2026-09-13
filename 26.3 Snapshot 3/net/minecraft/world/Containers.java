package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Containers {
   public static void dropContents(final Level level, final BlockPos pos, final Container container) {
      dropContents(level, pos.getX(), pos.getY(), pos.getZ(), container);
   }

   public static void dropContents(final Level level, final Entity entity, final Container container) {
      dropContents(level, entity.getX(), entity.getY(), entity.getZ(), container);
   }

   private static void dropContents(final Level level, final double x, final double y, final double z, final Container container) {
      for (int i = 0; i < container.getContainerSize(); i++) {
         dropItemStack(level, x, y, z, container.getItem(i));
      }
   }

   public static void dropContents(final Level level, final BlockPos pos, final NonNullList<ItemStack> list) {
      list.forEach(itemStack -> dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack));
   }

   public static void dropItemStack(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
      double size = EntityTypes.ITEM.getWidth();
      double centerRange = 1.0 - size;
      double halfSize = size / 2.0;
      RandomSource random = level.getRandom();
      double xo = Math.floor(x) + random.nextDouble() * centerRange + halfSize;
      double yo = Math.floor(y) + random.nextDouble() * centerRange;
      double zo = Math.floor(z) + random.nextDouble() * centerRange + halfSize;

      while (!itemStack.isEmpty()) {
         ItemEntity entity = new ItemEntity(level, xo, yo, zo, itemStack.split(random.nextInt(21) + 10));
         float pow = 0.05F;
         entity.setDeltaMovement(
            random.triangle(0.0, 0.11485000171139836), random.triangle(0.2, 0.11485000171139836), random.triangle(0.0, 0.11485000171139836)
         );
         level.addFreshEntity(entity);
      }
   }

   public static void updateNeighboursAfterDestroy(final BlockState state, final Level level, final BlockPos pos) {
      level.updateNeighbourForOutputSignal(pos, state.getBlock());
   }
}
