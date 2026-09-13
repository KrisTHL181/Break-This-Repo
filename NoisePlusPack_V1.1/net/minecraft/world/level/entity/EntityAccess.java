package net.minecraft.world.level.entity;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public interface EntityAccess extends UniquelyIdentifyable {
   int getId();

   BlockPos blockPosition();

   AABB getBoundingBox();

   void setLevelCallback(EntityInLevelCallback var1);

   Stream<? extends EntityAccess> getSelfAndPassengers();

   Stream<? extends EntityAccess> getPassengersAndSelf();

   void setRemoved(Entity.RemovalReason var1);

   boolean shouldBeSaved();

   boolean isAlwaysTicking();
}
