package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Boat extends AbstractBoat {
   public Boat(EntityType<? extends Boat> p_451130_, Level p_457363_, Supplier<Item> p_460270_) {
      super(p_451130_, p_457363_, p_460270_);
   }

   @Override
   protected double rideHeight(EntityDimensions p_450287_) {
      return p_450287_.height() / 3.0F;
   }
}
