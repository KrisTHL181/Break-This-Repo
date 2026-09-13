package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public record KineticWeapon(
   int contactCooldownTicks,
   int delayTicks,
   Optional<KineticWeapon.Condition> dismountConditions,
   Optional<KineticWeapon.Condition> knockbackConditions,
   Optional<KineticWeapon.Condition> damageConditions,
   float forwardMovement,
   float damageMultiplier,
   Optional<Holder<SoundEvent>> sound,
   Optional<Holder<SoundEvent>> hitSound
) {
   public static final int HIT_FEEDBACK_TICKS = 10;
   public static final Codec<KineticWeapon> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("contact_cooldown_ticks", 10).forGetter(KineticWeapon::contactCooldownTicks),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("delay_ticks", 0).forGetter(KineticWeapon::delayTicks),
            KineticWeapon.Condition.CODEC.optionalFieldOf("dismount_conditions").forGetter(KineticWeapon::dismountConditions),
            KineticWeapon.Condition.CODEC.optionalFieldOf("knockback_conditions").forGetter(KineticWeapon::knockbackConditions),
            KineticWeapon.Condition.CODEC.optionalFieldOf("damage_conditions").forGetter(KineticWeapon::damageConditions),
            Codec.FLOAT.optionalFieldOf("forward_movement", 0.0F).forGetter(KineticWeapon::forwardMovement),
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0F).forGetter(KineticWeapon::damageMultiplier),
            SoundEvent.CODEC.optionalFieldOf("sound").forGetter(KineticWeapon::sound),
            SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(KineticWeapon::hitSound)
         )
         .apply(i, KineticWeapon::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, KineticWeapon> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      KineticWeapon::contactCooldownTicks,
      ByteBufCodecs.VAR_INT,
      KineticWeapon::delayTicks,
      KineticWeapon.Condition.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::dismountConditions,
      KineticWeapon.Condition.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::knockbackConditions,
      KineticWeapon.Condition.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::damageConditions,
      ByteBufCodecs.FLOAT,
      KineticWeapon::forwardMovement,
      ByteBufCodecs.FLOAT,
      KineticWeapon::damageMultiplier,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::sound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::hitSound,
      KineticWeapon::new
   );

   public static Vec3 getMotion(Entity livingEntity) {
      if (!(livingEntity instanceof Player) && livingEntity.isPassenger()) {
         livingEntity = livingEntity.getRootVehicle();
      }

      return livingEntity.getKnownSpeed().scale(20.0);
   }

   public void makeSound(final Entity causer) {
      this.sound
         .ifPresent(
            s -> causer.level().playSound(causer, causer.getX(), causer.getY(), causer.getZ(), (Holder<SoundEvent>)s, causer.getSoundSource(), 1.0F, 1.0F)
         );
   }

   public void makeLocalHitSound(final Entity causer) {
      this.hitSound.ifPresent(s -> causer.level().playLocalSound(causer, s.value(), causer.getSoundSource(), 1.0F, 1.0F));
   }

   public int computeDamageUseDuration() {
      return this.delayTicks + this.damageConditions.map(KineticWeapon.Condition::maxDurationTicks).orElse(0);
   }

   public void damageEntities(final ItemStack stack, final int ticksRemaining, final LivingEntity livingEntity, final EquipmentSlot equipmentSlot) {
      int ticksUsed = stack.getUseDuration(livingEntity) - ticksRemaining;
      if (ticksUsed >= this.delayTicks) {
         ticksUsed -= this.delayTicks;
         Vec3 attackerLookVector = livingEntity.getLookAngle();
         double attackerSpeedProjection = attackerLookVector.dot(getMotion(livingEntity));
         float actionFactor = livingEntity instanceof Player ? 1.0F : 0.2F;
         AttackRange attackRange = livingEntity.getAttackRangeWith(stack);
         double baseMobDamage = livingEntity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
         boolean affected = false;

         for (EntityHitResult hitResult : (Collection)ProjectileUtil.getHitEntitiesAlong(
               livingEntity, attackRange, e -> PiercingWeapon.canHitEntity(livingEntity, e), ClipContext.Block.COLLIDER
            )
            .map(a -> List.of(), e -> e)) {
            Entity otherEntity = hitResult.getEntity();
            if (otherEntity instanceof EnderDragonPart dragonPart) {
               otherEntity = dragonPart.parentMob;
            }

            boolean wasStabbed = livingEntity.wasRecentlyStabbed(otherEntity, this.contactCooldownTicks);
            if (!wasStabbed) {
               livingEntity.rememberStabbedEntity(otherEntity);
               double targetSpeedProjection = attackerLookVector.dot(getMotion(otherEntity));
               double relativeSpeed = Math.max(0.0, attackerSpeedProjection - targetSpeedProjection);
               boolean dealsDismount = this.dismountConditions.isPresent()
                  && this.dismountConditions.get().test(ticksUsed, attackerSpeedProjection, relativeSpeed, actionFactor);
               boolean dealsKnockback = this.knockbackConditions.isPresent()
                  && this.knockbackConditions.get().test(ticksUsed, attackerSpeedProjection, relativeSpeed, actionFactor);
               boolean dealsDamage = this.damageConditions.isPresent()
                  && this.damageConditions.get().test(ticksUsed, attackerSpeedProjection, relativeSpeed, actionFactor);
               if (dealsDismount || dealsKnockback || dealsDamage) {
                  float damageDealt = (float)baseMobDamage + Mth.floor(relativeSpeed * this.damageMultiplier);
                  affected |= livingEntity.stabAttack(equipmentSlot, otherEntity, damageDealt, dealsDamage, dealsKnockback, dealsDismount);
               }
            }
         }

         if (affected) {
            livingEntity.level().broadcastEntityEvent(livingEntity, (byte)2);
            if (livingEntity instanceof ServerPlayer player) {
               CriteriaTriggers.SPEAR_MOBS_TRIGGER.trigger(player, livingEntity.stabbedEntities(e -> e instanceof LivingEntity));
            }
         }
      }
   }

   public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
      public static final Codec<KineticWeapon.Condition> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_duration_ticks").forGetter(KineticWeapon.Condition::maxDurationTicks),
               Codec.FLOAT.optionalFieldOf("min_speed", 0.0F).forGetter(KineticWeapon.Condition::minSpeed),
               Codec.FLOAT.optionalFieldOf("min_relative_speed", 0.0F).forGetter(KineticWeapon.Condition::minRelativeSpeed)
            )
            .apply(i, KineticWeapon.Condition::new)
      );
      public static final StreamCodec<ByteBuf, KineticWeapon.Condition> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         KineticWeapon.Condition::maxDurationTicks,
         ByteBufCodecs.FLOAT,
         KineticWeapon.Condition::minSpeed,
         ByteBufCodecs.FLOAT,
         KineticWeapon.Condition::minRelativeSpeed,
         KineticWeapon.Condition::new
      );

      public boolean test(final int ticksUsed, final double attackerSpeed, final double relativeSpeed, final double entityFactor) {
         return ticksUsed <= this.maxDurationTicks && attackerSpeed >= this.minSpeed * entityFactor && relativeSpeed >= this.minRelativeSpeed * entityFactor;
      }

      public static Optional<KineticWeapon.Condition> ofAttackerSpeed(final int untilTicks, final float minAttackerSpeed) {
         return Optional.of(new KineticWeapon.Condition(untilTicks, minAttackerSpeed, 0.0F));
      }

      public static Optional<KineticWeapon.Condition> ofRelativeSpeed(final int untilTicks, final float minRelativeSpeed) {
         return Optional.of(new KineticWeapon.Condition(untilTicks, 0.0F, minRelativeSpeed));
      }
   }
}
