package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record EntityFlagsPredicate(
   Optional<Boolean> isOnGround,
   Optional<Boolean> isOnFire,
   Optional<Boolean> isCrouching,
   Optional<Boolean> isSprinting,
   Optional<Boolean> isSwimming,
   Optional<Boolean> isFlying,
   Optional<Boolean> isBaby,
   Optional<Boolean> isInWater,
   Optional<Boolean> isFallFlying
) {
   public static final Codec<EntityFlagsPredicate> CODEC = RecordCodecBuilder.create(
      p_454876_ -> p_454876_.group(
            Codec.BOOL.optionalFieldOf("is_on_ground").forGetter(EntityFlagsPredicate::isOnGround),
            Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(EntityFlagsPredicate::isOnFire),
            Codec.BOOL.optionalFieldOf("is_sneaking").forGetter(EntityFlagsPredicate::isCrouching),
            Codec.BOOL.optionalFieldOf("is_sprinting").forGetter(EntityFlagsPredicate::isSprinting),
            Codec.BOOL.optionalFieldOf("is_swimming").forGetter(EntityFlagsPredicate::isSwimming),
            Codec.BOOL.optionalFieldOf("is_flying").forGetter(EntityFlagsPredicate::isFlying),
            Codec.BOOL.optionalFieldOf("is_baby").forGetter(EntityFlagsPredicate::isBaby),
            Codec.BOOL.optionalFieldOf("is_in_water").forGetter(EntityFlagsPredicate::isInWater),
            Codec.BOOL.optionalFieldOf("is_fall_flying").forGetter(EntityFlagsPredicate::isFallFlying)
         )
         .apply(p_454876_, EntityFlagsPredicate::new)
   );

   public boolean matches(Entity p_450142_) {
      if (this.isOnGround.isPresent() && p_450142_.onGround() != this.isOnGround.get()) {
         return false;
      }

      if (this.isOnFire.isPresent() && p_450142_.isOnFire() != this.isOnFire.get()) {
         return false;
      }

      if (this.isCrouching.isPresent() && p_450142_.isCrouching() != this.isCrouching.get()) {
         return false;
      }

      if (this.isSprinting.isPresent() && p_450142_.isSprinting() != this.isSprinting.get()) {
         return false;
      }

      if (this.isSwimming.isPresent() && p_450142_.isSwimming() != this.isSwimming.get()) {
         return false;
      }

      if (this.isFlying.isPresent()) {
         boolean flag = p_450142_ instanceof LivingEntity livingentity
            && (livingentity.isFallFlying() || livingentity instanceof Player player && player.getAbilities().flying);
         if (flag != this.isFlying.get()) {
            return false;
         }
      }

      if (this.isInWater.isPresent() && p_450142_.isInWater() != this.isInWater.get()) {
         return false;
      } else {
         return this.isFallFlying.isPresent() && p_450142_ instanceof LivingEntity livingentity1 && livingentity1.isFallFlying() != this.isFallFlying.get()
            ? false
            : !(this.isBaby.isPresent() && p_450142_ instanceof LivingEntity livingentity2) || livingentity2.isBaby() == this.isBaby.get();
      }
   }

   public static class Builder {
      private Optional<Boolean> isOnGround = Optional.empty();
      private Optional<Boolean> isOnFire = Optional.empty();
      private Optional<Boolean> isCrouching = Optional.empty();
      private Optional<Boolean> isSprinting = Optional.empty();
      private Optional<Boolean> isSwimming = Optional.empty();
      private Optional<Boolean> isFlying = Optional.empty();
      private Optional<Boolean> isBaby = Optional.empty();
      private Optional<Boolean> isInWater = Optional.empty();
      private Optional<Boolean> isFallFlying = Optional.empty();

      public static EntityFlagsPredicate.Builder flags() {
         return new EntityFlagsPredicate.Builder();
      }

      public EntityFlagsPredicate.Builder setOnGround(Boolean p_457039_) {
         this.isOnGround = Optional.of(p_457039_);
         return this;
      }

      public EntityFlagsPredicate.Builder setOnFire(Boolean p_453912_) {
         this.isOnFire = Optional.of(p_453912_);
         return this;
      }

      public EntityFlagsPredicate.Builder setCrouching(Boolean p_455313_) {
         this.isCrouching = Optional.of(p_455313_);
         return this;
      }

      public EntityFlagsPredicate.Builder setSprinting(Boolean p_452437_) {
         this.isSprinting = Optional.of(p_452437_);
         return this;
      }

      public EntityFlagsPredicate.Builder setSwimming(Boolean p_460402_) {
         this.isSwimming = Optional.of(p_460402_);
         return this;
      }

      public EntityFlagsPredicate.Builder setIsFlying(Boolean p_451102_) {
         this.isFlying = Optional.of(p_451102_);
         return this;
      }

      public EntityFlagsPredicate.Builder setIsBaby(Boolean p_450923_) {
         this.isBaby = Optional.of(p_450923_);
         return this;
      }

      public EntityFlagsPredicate.Builder setIsInWater(Boolean p_450315_) {
         this.isInWater = Optional.of(p_450315_);
         return this;
      }

      public EntityFlagsPredicate.Builder setIsFallFlying(Boolean p_454023_) {
         this.isFallFlying = Optional.of(p_454023_);
         return this;
      }

      public EntityFlagsPredicate build() {
         return new EntityFlagsPredicate(
            this.isOnGround, this.isOnFire, this.isCrouching, this.isSprinting, this.isSwimming, this.isFlying, this.isBaby, this.isInWater, this.isFallFlying
         );
      }
   }
}
