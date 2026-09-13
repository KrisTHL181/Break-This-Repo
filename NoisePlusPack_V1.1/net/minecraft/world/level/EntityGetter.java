package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface EntityGetter {
   List<Entity> getEntities(@Nullable Entity var1, AABB var2, Predicate<? super Entity> var3);

   <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> var1, AABB var2, Predicate<? super T> var3);

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> p_45979_, AABB p_45980_, Predicate<? super T> p_45981_) {
      return this.getEntities(EntityTypeTest.forClass(p_45979_), p_45980_, p_45981_);
   }

   List<? extends Player> players();

   default List<Entity> getEntities(@Nullable Entity p_45934_, AABB p_45935_) {
      return this.getEntities(p_45934_, p_45935_, EntitySelector.NO_SPECTATORS);
   }

   default boolean isUnobstructed(@Nullable Entity p_45939_, VoxelShape p_45940_) {
      if (p_45940_.isEmpty()) {
         return true;
      }

      for (Entity entity : this.getEntities(p_45939_, p_45940_.bounds())) {
         if (!entity.isRemoved()
            && entity.blocksBuilding
            && (p_45939_ == null || !entity.isPassengerOfSameVehicle(p_45939_))
            && Shapes.joinIsNotEmpty(p_45940_, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
            return false;
         }
      }

      return true;
   }

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> p_45977_, AABB p_45978_) {
      return this.getEntitiesOfClass(p_45977_, p_45978_, EntitySelector.NO_SPECTATORS);
   }

   default List<VoxelShape> getEntityCollisions(@Nullable Entity p_186451_, AABB p_186452_) {
      if (p_186452_.getSize() < 1.0E-7) {
         return List.of();
      }

      Predicate<Entity> predicate = p_186451_ == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(p_186451_::canCollideWith);
      List<Entity> list = this.getEntities(p_186451_, p_186452_.inflate(1.0E-7), predicate);
      if (list.isEmpty()) {
         return List.of();
      }

      Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

      for (Entity entity : list) {
         builder.add(Shapes.create(entity.getBoundingBox()));
      }

      return builder.build();
   }

   default @Nullable Player getNearestPlayer(double p_45919_, double p_45920_, double p_45921_, double p_45922_, @Nullable Predicate<Entity> p_45923_) {
      double d0 = -1.0;
      Player player = null;

      for (Player player1 : this.players()) {
         if (p_45923_ == null || p_45923_.test(player1)) {
            double d1 = player1.distanceToSqr(p_45919_, p_45920_, p_45921_);
            if ((p_45922_ < 0.0 || d1 < p_45922_ * p_45922_) && (d0 == -1.0 || d1 < d0)) {
               d0 = d1;
               player = player1;
            }
         }
      }

      return player;
   }

   default @Nullable Player getNearestPlayer(Entity p_45931_, double p_45932_) {
      return this.getNearestPlayer(p_45931_.getX(), p_45931_.getY(), p_45931_.getZ(), p_45932_, false);
   }

   default @Nullable Player getNearestPlayer(double p_45925_, double p_45926_, double p_45927_, double p_45928_, boolean p_45929_) {
      Predicate<Entity> predicate = p_45929_ ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
      return this.getNearestPlayer(p_45925_, p_45926_, p_45927_, p_45928_, predicate);
   }

   default boolean hasNearbyAlivePlayer(double p_45915_, double p_45916_, double p_45917_, double p_45918_) {
      for (Player player : this.players()) {
         if (EntitySelector.NO_SPECTATORS.test(player) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)) {
            double d0 = player.distanceToSqr(p_45915_, p_45916_, p_45917_);
            if (p_45918_ < 0.0 || d0 < p_45918_ * p_45918_) {
               return true;
            }
         }
      }

      return false;
   }

   default @Nullable Player getPlayerByUUID(UUID p_46004_) {
      for (int i = 0; i < this.players().size(); i++) {
         Player player = this.players().get(i);
         if (p_46004_.equals(player.getUUID())) {
            return player;
         }
      }

      return null;
   }
}
