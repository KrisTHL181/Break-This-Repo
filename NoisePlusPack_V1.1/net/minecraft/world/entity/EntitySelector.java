package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public final class EntitySelector {
   public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
   public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = p_20442_ -> p_20442_.isAlive() && p_20442_ instanceof LivingEntity;
   public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = p_20440_ -> p_20440_.isAlive() && !p_20440_.isVehicle() && !p_20440_.isPassenger();
   public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = p_20438_ -> p_20438_ instanceof Container && p_20438_.isAlive();
   public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = p_390489_ -> !(
      p_390489_ instanceof Player player && (p_390489_.isSpectator() || player.isCreative())
   );
   public static final Predicate<Entity> NO_SPECTATORS = p_20434_ -> !p_20434_.isSpectator();
   public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS.and(p_405267_ -> p_405267_.canBeCollidedWith(null));
   public static final Predicate<Entity> CAN_BE_PICKED = NO_SPECTATORS.and(Entity::isPickable);

   private EntitySelector() {
   }

   public static Predicate<Entity> withinDistance(double p_20411_, double p_20412_, double p_20413_, double p_20414_) {
      double d0 = p_20414_ * p_20414_;
      return p_449387_ -> p_449387_.distanceToSqr(p_20411_, p_20412_, p_20413_) <= d0;
   }

   public static Predicate<Entity> pushableBy(Entity p_20422_) {
      Team team = p_20422_.getTeam();
      Team.CollisionRule team$collisionrule = team == null ? Team.CollisionRule.ALWAYS : team.getCollisionRule();
      return (Predicate<Entity>)(team$collisionrule == Team.CollisionRule.NEVER
         ? Predicates.alwaysFalse()
         : NO_SPECTATORS.and(
            p_390493_ -> {
               if (!p_390493_.isPushable()) {
                  return false;
               }

               if (!p_20422_.level().isClientSide() || p_390493_ instanceof Player player && player.isLocalPlayer()) {
                  Team team1 = p_390493_.getTeam();
                  Team.CollisionRule team$collisionrule1 = team1 == null ? Team.CollisionRule.ALWAYS : team1.getCollisionRule();
                  if (team$collisionrule1 == Team.CollisionRule.NEVER) {
                     return false;
                  }

                  boolean flag = team != null && team.isAlliedTo(team1);
                  return (team$collisionrule == Team.CollisionRule.PUSH_OWN_TEAM || team$collisionrule1 == Team.CollisionRule.PUSH_OWN_TEAM) && flag
                     ? false
                     : team$collisionrule != Team.CollisionRule.PUSH_OTHER_TEAMS && team$collisionrule1 != Team.CollisionRule.PUSH_OTHER_TEAMS || flag;
               } else {
                  return false;
               }
            }
         ));
   }

   public static Predicate<Entity> notRiding(Entity p_20432_) {
      return p_20425_ -> {
         while (p_20425_.isPassenger()) {
            p_20425_ = p_20425_.getVehicle();
            if (p_20425_ == p_20432_) {
               return false;
            }
         }

         return true;
      };
   }
}
