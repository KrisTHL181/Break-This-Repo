package net.minecraft.world.entity.monster.piglin;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class AbstractPiglin extends Monster {
   protected static final EntityDataAccessor<Boolean> DATA_IMMUNE_TO_ZOMBIFICATION = SynchedEntityData.defineId(
      AbstractPiglin.class, EntityDataSerializers.BOOLEAN
   );
   public static final int CONVERSION_TIME = 300;
   private static final boolean DEFAULT_IMMUNE_TO_ZOMBIFICATION = false;
   private static final boolean DEFAULT_PICK_UP_LOOT = true;
   private static final int DEFAULT_TIME_IN_OVERWORLD = 0;
   protected int timeInOverworld = 0;

   public AbstractPiglin(final EntityType<? extends AbstractPiglin> type, final Level level) {
      super(type, level);
      this.setCanPickUpLoot(true);
      this.applyOpenDoorsAbility();
      this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0F);
      this.setPathfindingMalus(PathType.FIRE, -1.0F);
   }

   private void applyOpenDoorsAbility() {
      if (GoalUtils.hasGroundPathNavigation(this)) {
         this.getNavigation().setCanOpenDoors(true);
      }
   }

   protected abstract boolean canHunt();

   public void setImmuneToZombification(final boolean isImmuneToZombification) {
      this.getEntityData().set(DATA_IMMUNE_TO_ZOMBIFICATION, isImmuneToZombification);
   }

   protected boolean isImmuneToZombification() {
      return this.getEntityData().get(DATA_IMMUNE_TO_ZOMBIFICATION);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_IMMUNE_TO_ZOMBIFICATION, false);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("IsImmuneToZombification", this.isImmuneToZombification());
      output.putInt("TimeInOverworld", this.timeInOverworld);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setCanPickUpLoot(input.getBooleanOr("CanPickUpLoot", true));
      this.setImmuneToZombification(input.getBooleanOr("IsImmuneToZombification", false));
      this.timeInOverworld = input.getIntOr("TimeInOverworld", 0);
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      super.customServerAiStep(level);
      if (this.isConverting()) {
         this.timeInOverworld++;
      } else {
         this.timeInOverworld = 0;
      }

      if (this.timeInOverworld > 300) {
         if (level.getDifficulty() != Difficulty.PEACEFUL) {
            this.playConvertedSound();
         }

         this.finishConversion(level);
      }
   }

   @VisibleForTesting
   public void setTimeInOverworld(final int timeInOverworld) {
      this.timeInOverworld = timeInOverworld;
   }

   public boolean isConverting() {
      return !this.isImmuneToZombification()
         && !this.isNoAi()
         && this.level().environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, this.position());
   }

   protected void finishConversion(final ServerLevel level) {
      this.convertTo(
         EntityTypes.ZOMBIFIED_PIGLIN,
         ConversionParams.single(this, true, true),
         zombified -> zombified.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0))
      );
   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   public abstract PiglinArmPose getArmPose();

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   protected boolean isHoldingMeleeWeapon() {
      return this.getMainHandItem().has(DataComponents.TOOL);
   }

   @Override
   public void playAmbientSound() {
      if (PiglinAi.isIdle(this)) {
         super.playAmbientSound();
      }
   }

   protected abstract void playConvertedSound();
}
