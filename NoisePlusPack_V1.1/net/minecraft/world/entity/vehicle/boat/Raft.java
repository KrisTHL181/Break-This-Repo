package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Raft extends AbstractBoat {
   public Raft(EntityType<? extends Raft> p_453161_, Level p_451278_, Supplier<Item> p_458638_) {
      super(p_453161_, p_451278_, p_458638_);
   }

   @Override
   protected double rideHeight(EntityDimensions p_459805_) {
      return p_459805_.height() * 0.8888889F;
   }
}
