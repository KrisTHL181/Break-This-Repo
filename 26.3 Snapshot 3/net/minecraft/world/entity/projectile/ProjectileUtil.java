package net.minecraft.world.entity.projectile;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ProjectileUtil {
   public static final float DEFAULT_ENTITY_HIT_RESULT_MARGIN = 0.3F;

   public static HitResult getHitResultOnMoveVector(final Entity source, final Predicate<Entity> matching) {
      Vec3 movement = source.getDeltaMovement();
      Level level = source.level();
      Vec3 from = source.position();
      return getHitResult(from, source, matching, movement, level, computeMargin(source), ClipContext.Block.COLLIDER);
   }

   public static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(
      final Entity attacker, final AttackRange attackRange, final Predicate<Entity> matching, final ClipContext.Block blockClipType
   ) {
      Vec3 look = attacker.getHeadLookAngle();
      Vec3 eyePosition = attacker.getEyePosition();
      Vec3 from = eyePosition.add(look.scale(attackRange.effectiveMinRange(attacker)));
      double movementComponent = attacker.getKnownMovement().dot(look);
      Vec3 to = eyePosition.add(look.scale(attackRange.effectiveMaxRange(attacker) + Math.max(0.0, movementComponent)));
      return getHitEntitiesAlong(attacker, eyePosition, from, matching, to, attackRange.hitboxMargin(), blockClipType);
   }

   public static HitResult getHitResultOnMoveVector(final Entity source, final Predicate<Entity> matching, final ClipContext.Block clipType) {
      Vec3 movement = source.getDeltaMovement();
      Level level = source.level();
      Vec3 from = source.position();
      return getHitResult(from, source, matching, movement, level, computeMargin(source), clipType);
   }

   public static HitResult getHitResultOnViewVector(final Entity source, final Predicate<Entity> matching, final double distance) {
      Vec3 viewVector = source.getViewVector(0.0F).scale(distance);
      Level level = source.level();
      Vec3 from = source.getEyePosition();
      return getHitResult(from, source, matching, viewVector, level, 0.0F, ClipContext.Block.COLLIDER);
   }

   private static HitResult getHitResult(
      final Vec3 from,
      final Entity source,
      final Predicate<Entity> matching,
      final Vec3 delta,
      final Level level,
      final float entityMargin,
      final ClipContext.Block clipType
   ) {
      Vec3 to = from.add(delta);
      HitResult hitResult = level.clipIncludingBorder(new ClipContext(from, to, clipType, ClipContext.Fluid.NONE, source));
      if (hitResult.getType() != HitResult.Type.MISS) {
         to = hitResult.getLocation();
      }

      HitResult entityHit = getEntityHitResult(level, source, from, to, source.getBoundingBox().expandTowards(delta).inflate(1.0), matching, entityMargin);
      if (entityHit != null) {
         hitResult = entityHit;
      }

      return hitResult;
   }

   private static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(
      final Entity source,
      final Vec3 origin,
      final Vec3 from,
      final Predicate<Entity> matching,
      Vec3 to,
      final float entityMargin,
      final ClipContext.Block clipType
   ) {
      Level level = source.level();
      BlockHitResult hitResult = level.clipIncludingBorder(new ClipContext(origin, to, clipType, ClipContext.Fluid.NONE, source));
      if (hitResult.getType() != HitResult.Type.MISS) {
         to = hitResult.getLocation();
         if (origin.distanceToSqr(to) < origin.distanceToSqr(from)) {
            return Either.left(hitResult);
         }
      }

      AABB searchArea = AABB.ofSize(from, entityMargin, entityMargin, entityMargin).expandTowards(to.subtract(from)).inflate(1.0);
      Collection<EntityHitResult> entityHit = getManyEntityHitResult(level, source, from, to, searchArea, matching, entityMargin, clipType, true);
      return !entityHit.isEmpty() ? Either.right(entityHit) : Either.left(hitResult);
   }

   public static @Nullable EntityHitResult getEntityHitResult(
      final Entity except, final Vec3 from, final Vec3 to, final AABB box, final Predicate<Entity> matching, final double maxValue
   ) {
      Level level = except.level();
      double nearest = maxValue;
      Entity hovered = null;
      Vec3 hoveredPos = null;

      for (Entity entity : level.getEntities(except, box, matching)) {
         AABB bb = entity.getBoundingBox().inflate(entity.getPickRadius());
         Optional<Vec3> clipPoint = bb.clip(from, to);
         if (bb.contains(from)) {
            if (nearest >= 0.0 && entity.canBePickedFromInside()) {
               hovered = entity;
               hoveredPos = clipPoint.orElse(from);
               nearest = 0.0;
            }
         } else if (clipPoint.isPresent()) {
            Vec3 location = clipPoint.get();
            double dd = from.distanceToSqr(location);
            if (dd < nearest || nearest == 0.0) {
               if (entity.getRootVehicle() == except.getRootVehicle()) {
                  if (nearest == 0.0) {
                     hovered = entity;
                     hoveredPos = location;
                  }
               } else {
                  hovered = entity;
                  hoveredPos = location;
                  nearest = dd;
               }
            }
         }
      }

      return hovered == null ? null : new EntityHitResult(hovered, hoveredPos);
   }

   public static @Nullable EntityHitResult getEntityHitResult(
      final Level level, final Projectile source, final Vec3 from, final Vec3 to, final AABB targetSearchArea, final Predicate<Entity> matching
   ) {
      return getEntityHitResult(level, source, from, to, targetSearchArea, matching, computeMargin(source));
   }

   public static float computeMargin(final Entity source) {
      return Math.max(0.0F, Math.min(0.3F, (source.tickCount - 2) / 20.0F));
   }

   public static @Nullable EntityHitResult getEntityHitResult(
      final Level level,
      final Entity source,
      final Vec3 from,
      final Vec3 to,
      final AABB targetSearchArea,
      final Predicate<Entity> matching,
      final float entityMargin
   ) {
      double nearest = Double.MAX_VALUE;
      Optional<Vec3> nearestLocation = Optional.empty();
      Entity hitEntity = null;

      for (Entity entity : level.getEntities(source, targetSearchArea, matching)) {
         AABB bb = entity.getBoundingBox().inflate(entityMargin);
         Optional<Vec3> location = bb.clip(from, to);
         if (location.isPresent()) {
            double dd = from.distanceToSqr(location.get());
            if (dd < nearest) {
               hitEntity = entity;
               nearest = dd;
               nearestLocation = location;
            }
         }
      }

      return hitEntity == null ? null : new EntityHitResult(hitEntity, nearestLocation.get());
   }

   public static Collection<EntityHitResult> getManyEntityHitResult(
      final Level level,
      final Entity source,
      final Vec3 from,
      final Vec3 to,
      final AABB targetSearchArea,
      final Predicate<Entity> matching,
      final boolean includeFromEntity
   ) {
      return getManyEntityHitResult(level, source, from, to, targetSearchArea, matching, computeMargin(source), ClipContext.Block.COLLIDER, includeFromEntity);
   }

   public static Collection<EntityHitResult> getManyEntityHitResult(
      final Level level,
      final Entity source,
      final Vec3 from,
      final Vec3 to,
      final AABB targetSearchArea,
      final Predicate<Entity> matching,
      final float entityMargin,
      final ClipContext.Block clipType,
      final boolean includeFromEntity
   ) {
      List<EntityHitResult> collector = new ArrayList<>();

      for (Entity entity : level.getEntities(source, targetSearchArea, matching)) {
         AABB entityBB = entity.getBoundingBox();
         if (includeFromEntity && entityBB.contains(from)) {
            collector.add(new EntityHitResult(entity, from));
         } else {
            Optional<Vec3> exactHit = entityBB.clip(from, to);
            if (exactHit.isPresent()) {
               collector.add(new EntityHitResult(entity, exactHit.get()));
            } else if (!(entityMargin <= 0.0)) {
               Optional<Vec3> outsideHit = entityBB.inflate(entityMargin).clip(from, to);
               if (!outsideHit.isEmpty()) {
                  Vec3 outsideHitPosition = outsideHit.get();
                  Vec3 towardsTarget = entityBB.getCenter();
                  BlockHitResult hitResult = level.clipIncludingBorder(
                     new ClipContext(outsideHitPosition, towardsTarget, clipType, ClipContext.Fluid.NONE, source)
                  );
                  if (hitResult.getType() != HitResult.Type.MISS) {
                     towardsTarget = hitResult.getLocation();
                  }

                  Optional<Vec3> surfaceHit = entity.getBoundingBox().clip(outsideHitPosition, towardsTarget);
                  if (surfaceHit.isPresent()) {
                     collector.add(new EntityHitResult(entity, surfaceHit.get()));
                  }
               }
            }
         }
      }

      return collector;
   }

   public static void rotateTowardsMovement(final Entity projectile, final float rotationSpeed) {
      Vec3 movement = projectile.getDeltaMovement();
      if (movement.lengthSqr() != 0.0) {
         double sd = movement.horizontalDistance();
         projectile.setYRot((float)(Mth.atan2(movement.z, movement.x) * 180.0F / (float)Math.PI) + 90.0F);
         projectile.setXRot((float)(Mth.atan2(sd, movement.y) * 180.0F / (float)Math.PI) - 90.0F);

         while (projectile.getXRot() - projectile.xRotO < -180.0F) {
            projectile.xRotO -= 360.0F;
         }

         while (projectile.getXRot() - projectile.xRotO >= 180.0F) {
            projectile.xRotO += 360.0F;
         }

         while (projectile.getYRot() - projectile.yRotO < -180.0F) {
            projectile.yRotO -= 360.0F;
         }

         while (projectile.getYRot() - projectile.yRotO >= 180.0F) {
            projectile.yRotO += 360.0F;
         }

         projectile.setXRot(Mth.lerp(rotationSpeed, projectile.xRotO, projectile.getXRot()));
         projectile.setYRot(Mth.lerp(rotationSpeed, projectile.yRotO, projectile.getYRot()));
      }
   }

   public static InteractionHand getWeaponHoldingHand(final LivingEntity mob, final Item weaponItem) {
      return mob.getMainHandItem().is(weaponItem) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
   }

   public static AbstractArrow getMobArrow(final LivingEntity mob, final ItemStack projectile, final float power, final @Nullable ItemStack firedFromWeapon) {
      ArrowItem arrowItem = (ArrowItem)(projectile.getItem() instanceof ArrowItem ? projectile.getItem() : Items.ARROW);
      AbstractArrow arrow = arrowItem.createArrow(mob.level(), projectile, mob, firedFromWeapon);
      arrow.setBaseDamageFromMob(power);
      return arrow;
   }
}
