package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestRaft extends AbstractChestBoat {
   public ChestRaft(final EntityType<? extends ChestRaft> type, final Level level, final Supplier<Item> dropItem) {
      super(type, level, dropItem);
   }

   @Override
   protected double rideHeight(final EntityDimensions dimensions) {
      return dimensions.height() * 0.8888889F;
   }
}
