package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface ServerEntityGetter extends EntityGetter {
   ServerLevel getLevel();

   default @Nullable Player getNearestPlayer(final TargetingConditions targetConditions, final LivingEntity source) {
      return this.getNearestEntity(this.players(), targetConditions, source, source.getX(), source.getY(), source.getZ());
   }

   default @Nullable Player getNearestPlayer(
      final TargetingConditions targetConditions, final LivingEntity source, final double x, final double y, final double z
   ) {
      return this.getNearestEntity(this.players(), targetConditions, source, x, y, z);
   }

   default @Nullable Player getNearestPlayer(final TargetingConditions targetConditions, final double x, final double y, final double z) {
      return this.getNearestEntity(this.players(), targetConditions, null, x, y, z);
   }

   default <T extends LivingEntity> @Nullable T getNearestEntity(
      final Class<? extends T> type,
      final TargetingConditions targetConditions,
      final @Nullable LivingEntity source,
      final double x,
      final double y,
      final double z,
      final AABB bb
   ) {
      return this.getNearestEntity(this.getEntitiesOfClass(type, bb, entity -> true), targetConditions, source, x, y, z);
   }

   default @Nullable LivingEntity getNearestEntity(
      final TagKey<EntityType<?>> tag,
      final TargetingConditions targetConditions,
      final @Nullable LivingEntity source,
      final double x,
      final double y,
      final double z,
      final AABB bb
   ) {
      double bestDistance = Double.MAX_VALUE;
      LivingEntity nearestEntity = null;

      for (LivingEntity entity : this.getEntitiesOfClass(LivingEntity.class, bb, e -> e.is(tag))) {
         if (targetConditions.test(this.getLevel(), source, entity)) {
            double distance = entity.distanceToSqr(x, y, z);
            if (distance < bestDistance) {
               bestDistance = distance;
               nearestEntity = entity;
            }
         }
      }

      return nearestEntity;
   }

   default <T extends Entity> @Nullable T getNearestEntity(final List<? extends T> entities, final double x, final double y, final double z) {
      double best = -1.0;
      T result = null;

      for (T entity : entities) {
         double dist = entity.distanceToSqr(x, y, z);
         if (best == -1.0 || dist < best) {
            best = dist;
            result = entity;
         }
      }

      return result;
   }

   default <T extends LivingEntity> @Nullable T getNearestEntity(
      final List<? extends T> entities,
      final TargetingConditions targetConditions,
      final @Nullable LivingEntity source,
      final double x,
      final double y,
      final double z
   ) {
      double best = -1.0;
      T result = null;

      for (T entity : entities) {
         if (targetConditions.test(this.getLevel(), source, entity)) {
            double dist = entity.distanceToSqr(x, y, z);
            if (best == -1.0 || dist < best) {
               best = dist;
               result = entity;
            }
         }
      }

      return result;
   }

   default List<Player> getNearbyPlayers(final TargetingConditions targetConditions, final LivingEntity source, final AABB bb) {
      List<Player> foundPlayers = new ArrayList<>();

      for (Player player : this.players()) {
         if (bb.contains(player.getX(), player.getY(), player.getZ()) && targetConditions.test(this.getLevel(), source, player)) {
            foundPlayers.add(player);
         }
      }

      return foundPlayers;
   }

   default <T extends LivingEntity> List<T> getNearbyEntities(
      final Class<T> type, final TargetingConditions targetConditions, final LivingEntity source, final AABB bb
   ) {
      List<T> nearby = this.getEntitiesOfClass(type, bb, entityx -> true);
      List<T> entities = new ArrayList<>();

      for (T entity : nearby) {
         if (targetConditions.test(this.getLevel(), source, entity)) {
            entities.add(entity);
         }
      }

      return entities;
   }
}
