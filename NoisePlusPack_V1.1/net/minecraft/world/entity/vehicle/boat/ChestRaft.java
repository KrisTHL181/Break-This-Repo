package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestRaft extends AbstractChestBoat {
   public ChestRaft(EntityType<? extends ChestRaft> p_454835_, Level p_458215_, Supplier<Item> p_453308_) {
      super(p_454835_, p_458215_, p_453308_);
   }

   @Override
   protected double rideHeight(EntityDimensions p_458718_) {
      return p_458718_.height() * 0.8888889F;
   }
}
