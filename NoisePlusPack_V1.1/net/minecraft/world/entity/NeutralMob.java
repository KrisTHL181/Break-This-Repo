package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public interface NeutralMob {
   String TAG_ANGER_END_TIME = "anger_end_time";
   String TAG_ANGRY_AT = "angry_at";
   long NO_ANGER_END_TIME = -1L;

   long getPersistentAngerEndTime();

   default void setTimeToRemainAngry(long p_454333_) {
      this.setPersistentAngerEndTime(this.level().getGameTime() + p_454333_);
   }

   void setPersistentAngerEndTime(long var1);

   @Nullable EntityReference<LivingEntity> getPersistentAngerTarget();

   void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> var1);

   void startPersistentAngerTimer();

   Level level();

   default void addPersistentAngerSaveData(ValueOutput p_407892_) {
      p_407892_.putLong("anger_end_time", this.getPersistentAngerEndTime());
      p_407892_.storeNullable("angry_at", EntityReference.codec(), this.getPersistentAngerTarget());
   }

   default void readPersistentAngerSaveData(Level p_147286_, ValueInput p_410075_) {
      Optional<Long> optional = p_410075_.getLong("anger_end_time");
      if (optional.isPresent()) {
         this.setPersistentAngerEndTime(optional.get());
      } else {
         Optional<Integer> optional1 = p_410075_.getInt("AngerTime");
         if (optional1.isPresent()) {
            this.setTimeToRemainAngry(optional1.get().intValue());
         } else {
            this.setPersistentAngerEndTime(-1L);
         }
      }

      if (p_147286_ instanceof ServerLevel) {
         this.setPersistentAngerTarget(EntityReference.read(p_410075_, "angry_at"));
         this.setTarget(EntityReference.getLivingEntity(this.getPersistentAngerTarget(), p_147286_));
      }
   }

   default void updatePersistentAnger(ServerLevel p_21667_, boolean p_21668_) {
      LivingEntity livingentity = this.getTarget();
      EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
      if (livingentity != null
         && livingentity.isDeadOrDying()
         && entityreference != null
         && entityreference.matches(livingentity)
         && livingentity instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if (livingentity != null) {
            if (entityreference == null || !entityreference.matches(livingentity)) {
               this.setPersistentAngerTarget(EntityReference.of(livingentity));
            }

            this.startPersistentAngerTimer();
         }

         if (entityreference != null && !this.isAngry() && (livingentity == null || !isValidPlayerTarget(livingentity) || !p_21668_)) {
            this.stopBeingAngry();
         }
      }
   }

   private static boolean isValidPlayerTarget(LivingEntity p_456578_) {
      return p_456578_ instanceof Player player && !player.isCreative() && !player.isSpectator();
   }

   default boolean isAngryAt(LivingEntity p_21675_, ServerLevel p_366229_) {
      if (!this.canAttack(p_21675_)) {
         return false;
      }

      if (isValidPlayerTarget(p_21675_) && this.isAngryAtAllPlayers(p_366229_)) {
         return true;
      }

      EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
      return entityreference != null && entityreference.matches(p_21675_);
   }

   default boolean isAngryAtAllPlayers(ServerLevel p_362225_) {
      return p_362225_.getGameRules().get(GameRules.UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
   }

   default boolean isAngry() {
      long i = this.getPersistentAngerEndTime();
      if (i > 0L) {
         long j = i - this.level().getGameTime();
         return j > 0L;
      } else {
         return false;
      }
   }

   default void playerDied(ServerLevel p_360871_, Player p_21677_) {
      if (p_360871_.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS)) {
         EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
         if (entityreference != null && entityreference.matches(p_21677_)) {
            this.stopBeingAngry();
         }
      }
   }

   default void forgetCurrentTargetAndRefreshUniversalAnger() {
      this.stopBeingAngry();
      this.startPersistentAngerTimer();
   }

   default void stopBeingAngry() {
      this.setLastHurtByMob(null);
      this.setPersistentAngerTarget(null);
      this.setTarget(null);
      this.setPersistentAngerEndTime(-1L);
   }

   @Nullable LivingEntity getLastHurtByMob();

   void setLastHurtByMob(@Nullable LivingEntity var1);

   void setTarget(@Nullable LivingEntity var1);

   boolean canAttack(LivingEntity var1);

   @Nullable LivingEntity getTarget();
}
