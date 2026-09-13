package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;

public enum ConversionType {
   SINGLE(true) {
      @Override
      void convert(Mob p_362402_, Mob p_366485_, ConversionParams p_364039_) {
         Entity entity = p_362402_.getFirstPassenger();
         p_366485_.copyPosition(p_362402_);
         p_366485_.setDeltaMovement(p_362402_.getDeltaMovement());
         if (entity != null) {
            entity.stopRiding();
            entity.boardingCooldown = 0;

            for (Entity entity1 : p_366485_.getPassengers()) {
               entity1.stopRiding();
               entity1.remove(Entity.RemovalReason.DISCARDED);
            }

            entity.startRiding(p_366485_);
         }

         Entity entity2 = p_362402_.getVehicle();
         if (entity2 != null) {
            p_362402_.stopRiding();
            p_366485_.startRiding(entity2, false, false);
         }

         if (p_364039_.keepEquipment()) {
            for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
               ItemStack itemstack = p_362402_.getItemBySlot(equipmentslot);
               if (!itemstack.isEmpty()) {
                  p_366485_.setItemSlot(equipmentslot, itemstack.copyAndClear());
                  p_366485_.setDropChance(equipmentslot, p_362402_.getDropChances().byEquipment(equipmentslot));
               }
            }
         }

         p_366485_.fallDistance = p_362402_.fallDistance;
         p_366485_.setSharedFlag(7, p_362402_.isFallFlying());
         p_366485_.lastHurtByPlayerMemoryTime = p_362402_.lastHurtByPlayerMemoryTime;
         p_366485_.hurtTime = p_362402_.hurtTime;
         p_366485_.yBodyRot = p_362402_.yBodyRot;
         p_366485_.setOnGround(p_362402_.onGround());
         p_362402_.getSleepingPos().ifPresent(p_366485_::setSleepingPos);
         Entity entity3 = p_362402_.getLeashHolder();
         if (entity3 != null) {
            p_366485_.setLeashedTo(entity3, true);
         }

         this.convertCommon(p_362402_, p_366485_, p_364039_);
      }
   },
   SPLIT_ON_DEATH(false) {
      @Override
      void convert(Mob p_362122_, Mob p_361715_, ConversionParams p_364524_) {
         Entity entity = p_362122_.getFirstPassenger();
         if (entity != null) {
            entity.stopRiding();
         }

         Entity entity1 = p_362122_.getLeashHolder();
         if (entity1 != null) {
            p_362122_.dropLeash();
         }

         this.convertCommon(p_362122_, p_361715_, p_364524_);
      }
   };

   private static final Set<DataComponentType<?>> COMPONENTS_TO_COPY = Set.of(DataComponents.CUSTOM_NAME, DataComponents.CUSTOM_DATA);
   private final boolean discardAfterConversion;

   ConversionType(final boolean p_361780_) {
      this.discardAfterConversion = p_361780_;
   }

   public boolean shouldDiscardAfterConversion() {
      return this.discardAfterConversion;
   }

   abstract void convert(Mob var1, Mob var2, ConversionParams var3);

   void convertCommon(Mob p_368736_, Mob p_363577_, ConversionParams p_361619_) {
      p_363577_.setAbsorptionAmount(p_368736_.getAbsorptionAmount());

      for (MobEffectInstance mobeffectinstance : p_368736_.getActiveEffects()) {
         p_363577_.addEffect(new MobEffectInstance(mobeffectinstance));
      }

      if (p_368736_.isBaby()) {
         p_363577_.setBaby(true);
      }

      if (p_368736_ instanceof AgeableMob ageablemob && p_363577_ instanceof AgeableMob ageablemob1) {
         ageablemob1.setAge(ageablemob.getAge());
         ageablemob1.forcedAge = ageablemob.forcedAge;
         ageablemob1.forcedAgeTimer = ageablemob.forcedAgeTimer;
      }

      Brain<?> brain = p_368736_.getBrain();
      Brain<?> brain1 = p_363577_.getBrain();
      if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED) && brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
         brain1.setMemory(MemoryModuleType.ANGRY_AT, brain.getMemory(MemoryModuleType.ANGRY_AT));
      }

      if (p_361619_.preserveCanPickUpLoot()) {
         p_363577_.setCanPickUpLoot(p_368736_.canPickUpLoot());
      }

      p_363577_.setLeftHanded(p_368736_.isLeftHanded());
      p_363577_.setNoAi(p_368736_.isNoAi());
      if (p_368736_.isPersistenceRequired()) {
         p_363577_.setPersistenceRequired();
      }

      p_363577_.setCustomNameVisible(p_368736_.isCustomNameVisible());
      p_363577_.setSharedFlagOnFire(p_368736_.isOnFire());
      p_363577_.setInvulnerable(p_368736_.isInvulnerable());
      p_363577_.setNoGravity(p_368736_.isNoGravity());
      p_363577_.setPortalCooldown(p_368736_.getPortalCooldown());
      p_363577_.setSilent(p_368736_.isSilent());
      p_368736_.getTags().forEach(p_363577_::addTag);

      for (DataComponentType<?> datacomponenttype : COMPONENTS_TO_COPY) {
         copyComponent(p_368736_, p_363577_, datacomponenttype);
      }

      if (p_361619_.team() != null) {
         Scoreboard scoreboard = p_363577_.level().getScoreboard();
         scoreboard.addPlayerToTeam(p_363577_.getStringUUID(), p_361619_.team());
         if (p_368736_.getTeam() != null && p_368736_.getTeam() == p_361619_.team()) {
            scoreboard.removePlayerFromTeam(p_368736_.getStringUUID(), p_368736_.getTeam());
         }
      }

      if (p_368736_ instanceof Zombie zombie && zombie.canBreakDoors() && p_363577_ instanceof Zombie zombie1) {
         zombie1.setCanBreakDoors(true);
      }
   }

   private static <T> void copyComponent(Mob p_410722_, Mob p_406961_, DataComponentType<T> p_409658_) {
      T t = p_410722_.get(p_409658_);
      if (t != null) {
         p_406961_.setComponent(p_409658_, t);
      }
   }
}
