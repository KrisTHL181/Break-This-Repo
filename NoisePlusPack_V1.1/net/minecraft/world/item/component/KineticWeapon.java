package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
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
      p_452794_ -> p_452794_.group(
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
         .apply(p_452794_, KineticWeapon::new)
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

   public static Vec3 getMotion(Entity p_457234_) {
      if (!(p_457234_ instanceof Player) && p_457234_.isPassenger()) {
         p_457234_ = p_457234_.getRootVehicle();
      }

      return p_457234_.getKnownSpeed().scale(20.0);
   }

   public void makeSound(Entity p_450948_) {
      this.sound
         .ifPresent(
            p_457136_ -> p_450948_.level()
               .playSound(
                  p_450948_, p_450948_.getX(), p_450948_.getY(), p_450948_.getZ(), (Holder<SoundEvent>)p_457136_, p_450948_.getSoundSource(), 1.0F, 1.0F
               )
         );
   }

   public void makeLocalHitSound(Entity p_452945_) {
      this.hitSound.ifPresent(p_459160_ -> p_452945_.level().playLocalSound(p_452945_, p_459160_.value(), p_452945_.getSoundSource(), 1.0F, 1.0F));
   }

   public int computeDamageUseDuration() {
      return this.delayTicks + this.damageConditions.map(KineticWeapon.Condition::maxDurationTicks).orElse(0);
   }

   public void damageEntities(ItemStack p_456242_, int p_455000_, LivingEntity p_459158_, EquipmentSlot p_455231_) {
      int i = p_456242_.getUseDuration(p_459158_) - p_455000_;
      if (i >= this.delayTicks) {
         i -= this.delayTicks;
         Vec3 vec3 = p_459158_.getLookAngle();
         double d0 = vec3.dot(getMotion(p_459158_));
         float f = p_459158_ instanceof Player ? 1.0F : 0.2F;
         AttackRange attackrange = p_459158_.entityAttackRange();
         double d1 = p_459158_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
         boolean flag = false;

         for (EntityHitResult entityhitresult : (Collection)ProjectileUtil.getHitEntitiesAlong(
               p_459158_, attackrange, p_453233_ -> PiercingWeapon.canHitEntity(p_459158_, p_453233_), ClipContext.Block.COLLIDER
            )
            .map(p_458641_ -> List.of(), p_457803_ -> p_457803_)) {
            Entity entity = entityhitresult.getEntity();
            if (entity instanceof EnderDragonPart enderdragonpart) {
               entity = enderdragonpart.parentMob;
            }

            boolean flag4 = p_459158_.wasRecentlyStabbed(entity, this.contactCooldownTicks);
            if (!flag4) {
               p_459158_.rememberStabbedEntity(entity);
               double d2 = vec3.dot(getMotion(entity));
               double d3 = Math.max(0.0, d0 - d2);
               boolean flag1 = this.dismountConditions.isPresent() && this.dismountConditions.get().test(i, d0, d3, f);
               boolean flag2 = this.knockbackConditions.isPresent() && this.knockbackConditions.get().test(i, d0, d3, f);
               boolean flag3 = this.damageConditions.isPresent() && this.damageConditions.get().test(i, d0, d3, f);
               if (flag1 || flag2 || flag3) {
                  float f1 = (float)d1 + Mth.floor(d3 * this.damageMultiplier);
                  flag |= p_459158_.stabAttack(p_455231_, entity, f1, flag3, flag2, flag1);
               }
            }
         }

         if (flag) {
            p_459158_.level().broadcastEntityEvent(p_459158_, (byte)2);
            if (p_459158_ instanceof ServerPlayer serverplayer) {
               CriteriaTriggers.SPEAR_MOBS_TRIGGER.trigger(serverplayer, p_459158_.stabbedEntities(p_455528_ -> p_455528_ instanceof LivingEntity));
            }
         }
      }
   }

   public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
      public static final Codec<KineticWeapon.Condition> CODEC = RecordCodecBuilder.create(
         p_456918_ -> p_456918_.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_duration_ticks").forGetter(KineticWeapon.Condition::maxDurationTicks),
               Codec.FLOAT.optionalFieldOf("min_speed", 0.0F).forGetter(KineticWeapon.Condition::minSpeed),
               Codec.FLOAT.optionalFieldOf("min_relative_speed", 0.0F).forGetter(KineticWeapon.Condition::minRelativeSpeed)
            )
            .apply(p_456918_, KineticWeapon.Condition::new)
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

      public boolean test(int p_455471_, double p_460989_, double p_458370_, double p_457386_) {
         return p_455471_ <= this.maxDurationTicks && p_460989_ >= this.minSpeed * p_457386_ && p_458370_ >= this.minRelativeSpeed * p_457386_;
      }

      public static Optional<KineticWeapon.Condition> ofAttackerSpeed(int p_450624_, float p_456056_) {
         return Optional.of(new KineticWeapon.Condition(p_450624_, p_456056_, 0.0F));
      }

      public static Optional<KineticWeapon.Condition> ofRelativeSpeed(int p_457635_, float p_460567_) {
         return Optional.of(new KineticWeapon.Condition(p_457635_, 0.0F, p_460567_));
      }
   }
}
