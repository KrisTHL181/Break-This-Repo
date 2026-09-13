package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestBoat extends AbstractChestBoat {
   public ChestBoat(EntityType<? extends ChestBoat> p_455226_, Level p_456059_, Supplier<Item> p_455414_) {
      super(p_455226_, p_456059_, p_455414_);
   }

   @Override
   protected double rideHeight(EntityDimensions p_451474_) {
      return p_451474_.height() / 3.0F;
   }
}
