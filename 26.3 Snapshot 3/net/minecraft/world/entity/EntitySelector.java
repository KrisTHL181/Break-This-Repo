package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public final class EntitySelector {
   public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
   public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = entity -> entity.isAlive() && entity instanceof LivingEntity;
   public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = entity -> entity.isAlive() && !entity.isVehicle() && !entity.isPassenger();
   public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = entity -> entity instanceof Container && entity.isAlive();
   public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = entity -> !(
      entity instanceof Player player && (entity.isSpectator() || player.isCreative())
   );
   public static final Predicate<Entity> NO_SPECTATORS = entity -> !entity.isSpectator();
   public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS.and(entity -> entity.canBeCollidedWith(null));
   public static final Predicate<Entity> CAN_BE_PICKED = Entity::isPickable;

   private EntitySelector() {
   }

   public static Predicate<Entity> withinDistance(final double centerX, final double centerY, final double centerZ, final double distance) {
      double distanceSqr = distance * distance;
      return input -> input.distanceToSqr(centerX, centerY, centerZ) <= distanceSqr;
   }

   public static Predicate<Entity> pushableBy(final Entity entity) {
      Team ownTeam = entity.getTeam();
      Team.CollisionRule ownCollisionRule = ownTeam == null ? Team.CollisionRule.ALWAYS : ownTeam.getCollisionRule();
      return (Predicate<Entity>)(ownCollisionRule == Team.CollisionRule.NEVER
         ? Predicates.alwaysFalse()
         : NO_SPECTATORS.and(
            input -> {
               if (!input.isPushable()) {
                  return false;
               }

               if (!entity.level().isClientSide() || input instanceof Player player && player.isLocalPlayer()) {
                  Team theirTeam = input.getTeam();
                  Team.CollisionRule theirCollisionRule = theirTeam == null ? Team.CollisionRule.ALWAYS : theirTeam.getCollisionRule();
                  if (theirCollisionRule == Team.CollisionRule.NEVER) {
                     return false;
                  }

                  boolean sameTeam = ownTeam != null && ownTeam.isAlliedTo(theirTeam);
                  return (ownCollisionRule == Team.CollisionRule.PUSH_OWN_TEAM || theirCollisionRule == Team.CollisionRule.PUSH_OWN_TEAM) && sameTeam
                     ? false
                     : ownCollisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS && theirCollisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS || sameTeam;
               } else {
                  return false;
               }
            }
         ));
   }

   public static Predicate<Entity> notRiding(final Entity entity) {
      return input -> {
         while (input.isPassenger()) {
            input = input.getVehicle();
            if (input == entity) {
               return false;
            }
         }

         return true;
      };
   }
}
