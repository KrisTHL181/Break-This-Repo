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

public record AttackRange(float minRange, float maxRange, float minCreativeRange, float maxCreativeRange, float hitboxMargin, float mobFactor) {
   public static final Codec<AttackRange> CODEC = RecordCodecBuilder.create(
      p_454041_ -> p_454041_.group(
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("min_reach", 0.0F).forGetter(AttackRange::minRange),
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("max_reach", 3.0F).forGetter(AttackRange::maxRange),
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("min_creative_reach", 0.0F).forGetter(AttackRange::minCreativeRange),
            ExtraCodecs.floatRange(0.0F, 64.0F).optionalFieldOf("max_creative_reach", 5.0F).forGetter(AttackRange::maxCreativeRange),
            ExtraCodecs.floatRange(0.0F, 1.0F).optionalFieldOf("hitbox_margin", 0.3F).forGetter(AttackRange::hitboxMargin),
            Codec.floatRange(0.0F, 2.0F).optionalFieldOf("mob_factor", 1.0F).forGetter(AttackRange::mobFactor)
         )
         .apply(p_454041_, AttackRange::new)
   );
   public static final StreamCodec<ByteBuf, AttackRange> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      AttackRange::minRange,
      ByteBufCodecs.FLOAT,
      AttackRange::maxRange,
      ByteBufCodecs.FLOAT,
      AttackRange::minCreativeRange,
      ByteBufCodecs.FLOAT,
      AttackRange::maxCreativeRange,
      ByteBufCodecs.FLOAT,
      AttackRange::hitboxMargin,
      ByteBufCodecs.FLOAT,
      AttackRange::mobFactor,
      AttackRange::new
   );

   public static AttackRange defaultFor(LivingEntity p_456142_) {
      return new AttackRange(
         0.0F,
         (float)p_456142_.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),
         0.0F,
         (float)p_456142_.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),
         0.0F,
         1.0F
      );
   }

   public HitResult getClosesetHit(Entity p_460657_, float p_460900_, Predicate<Entity> p_458573_) {
      Either<BlockHitResult, Collection<EntityHitResult>> either = ProjectileUtil.getHitEntitiesAlong(p_460657_, this, p_458573_, ClipContext.Block.OUTLINE);
      if (either.left().isPresent()) {
         return (HitResult)either.left().get();
      }

      Collection<EntityHitResult> collection = (Collection<EntityHitResult>)either.right().get();
      EntityHitResult entityhitresult = null;
      Vec3 vec3 = p_460657_.getEyePosition(p_460900_);
      double d0 = Double.MAX_VALUE;

      for (EntityHitResult entityhitresult1 : collection) {
         double d1 = vec3.distanceToSqr(entityhitresult1.getLocation());
         if (d1 < d0) {
            d0 = d1;
            entityhitresult = entityhitresult1;
         }
      }

      if (entityhitresult != null) {
         return entityhitresult;
      }

      Vec3 vec31 = p_460657_.getHeadLookAngle();
      Vec3 vec32 = p_460657_.getEyePosition(p_460900_).add(vec31);
      return BlockHitResult.miss(vec32, Direction.getApproximateNearest(vec31), BlockPos.containing(vec32));
   }

   public float effectiveMinRange(Entity p_460694_) {
      if (p_460694_ instanceof Player player) {
         if (player.isSpectator()) {
            return 0.0F;
         } else {
            return player.isCreative() ? this.minCreativeRange : this.minRange;
         }
      } else {
         return this.minRange * this.mobFactor;
      }
   }

   public float effectiveMaxRange(Entity p_450477_) {
      if (p_450477_ instanceof Player player) {
         return player.isCreative() ? this.maxCreativeRange : this.maxRange;
      } else {
         return this.maxRange * this.mobFactor;
      }
   }

   public boolean isInRange(LivingEntity p_450206_, Vec3 p_456236_) {
      return this.isInRange(p_450206_, p_456236_::distanceToSqr, 0.0);
   }

   public boolean isInRange(LivingEntity p_460499_, AABB p_459371_, double p_459884_) {
      return this.isInRange(p_460499_, p_459371_::distanceToSqr, p_459884_);
   }

   private boolean isInRange(LivingEntity p_452636_, ToDoubleFunction<Vec3> p_457212_, double p_457293_) {
      double d0 = Math.sqrt(p_457212_.applyAsDouble(p_452636_.getEyePosition()));
      double d1 = this.effectiveMinRange(p_452636_) - this.hitboxMargin - p_457293_;
      double d2 = this.effectiveMaxRange(p_452636_) + this.hitboxMargin + p_457293_;
      return d0 >= d1 && d0 <= d2;
   }
}
