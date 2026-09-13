package net.minecraft.world.item.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record AttackRange(float minReach, float maxReach, float minCreativeReach, float maxCreativeReach, float hitboxMargin, float mobFactor) {
   public static final Codec<AttackRange> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("min_reach", 0.0F).forGetter(AttackRange::minReach),
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("max_reach", 3.0F).forGetter(AttackRange::maxReach),
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("min_creative_reach", 0.0F).forGetter(AttackRange::minCreativeReach),
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("max_creative_reach", 5.0F).forGetter(AttackRange::maxCreativeReach),
            ExtraCodecs.floatRange(0.0F, 1.0F).optionalFieldOf("hitbox_margin", 0.3F).forGetter(AttackRange::hitboxMargin),
            Codec.floatRange(0.0F, 2.0F).optionalFieldOf("mob_factor", 1.0F).forGetter(AttackRange::mobFactor)
         )
         .apply(i, AttackRange::new)
   );
   public static final StreamCodec<ByteBuf, AttackRange> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      AttackRange::minReach,
      ByteBufCodecs.FLOAT,
      AttackRange::maxReach,
      ByteBufCodecs.FLOAT,
      AttackRange::minCreativeReach,
      ByteBufCodecs.FLOAT,
      AttackRange::maxCreativeReach,
      ByteBufCodecs.FLOAT,
      AttackRange::hitboxMargin,
      ByteBufCodecs.FLOAT,
      AttackRange::mobFactor,
      AttackRange::new
   );

   public static AttackRange defaultFor(final LivingEntity livingEntity) {
      float interactionRange = (float)livingEntity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
      return new AttackRange(0.0F, interactionRange, 0.0F, interactionRange, 0.0F, 1.0F);
   }

   public HitResult getClosesetHit(final Entity attacker, final float partial, final Predicate<Entity> matching) {
      Either<BlockHitResult, Collection<EntityHitResult>> result = ProjectileUtil.getHitEntitiesAlong(attacker, this, matching, ClipContext.Block.OUTLINE);
      if (result.left().isPresent()) {
         return (HitResult)result.left().get();
      }

      Collection<EntityHitResult> targets = (Collection<EntityHitResult>)result.right().get();
      EntityHitResult entity = null;
      Vec3 attackerPos = attacker.getEyePosition(partial);
      double closestDistance = Double.MAX_VALUE;

      for (EntityHitResult target : targets) {
         double distance = attackerPos.distanceToSqr(target.getLocation());
         if (distance < closestDistance) {
            closestDistance = distance;
            entity = target;
         }
      }

      if (entity != null) {
         return entity;
      }

      Vec3 eyeGaze = attacker.getHeadLookAngle();
      Vec3 missPosition = attacker.getEyePosition(partial).add(eyeGaze);
      return BlockHitResult.miss(missPosition, Direction.getApproximateNearest(eyeGaze), BlockPos.containing(missPosition));
   }

   public float effectiveMinRange(final Entity entity) {
      if (entity instanceof Player player) {
         return player.isCreative() ? this.minCreativeReach : this.minReach;
      } else {
         return this.minReach * this.mobFactor;
      }
   }

   public float effectiveMaxRange(final Entity entity) {
      if (entity instanceof Player player) {
         return player.isCreative() ? this.maxCreativeReach : this.maxReach;
      } else {
         return this.maxReach * this.mobFactor;
      }
   }

   public boolean isInRange(final LivingEntity attacker, final Vec3 location) {
      return this.isInRange(attacker, location::distanceToSqr, 0.0);
   }

   public boolean isInRange(final LivingEntity attacker, final AABB boundingBox, final double extraBuffer) {
      return this.isInRange(attacker, boundingBox::distanceToSqr, extraBuffer);
   }

   private boolean isInRange(final LivingEntity attacker, final ToDoubleFunction<Vec3> distanceFunction, final double extraBuffer) {
      double distance = Math.sqrt(distanceFunction.applyAsDouble(attacker.getEyePosition()));
      double minReach = this.effectiveMinRange(attacker) - this.hitboxMargin - extraBuffer;
      double maxReach = this.effectiveMaxRange(attacker) + this.hitboxMargin + extraBuffer;
      return distance >= minReach && distance <= maxReach;
   }
}
