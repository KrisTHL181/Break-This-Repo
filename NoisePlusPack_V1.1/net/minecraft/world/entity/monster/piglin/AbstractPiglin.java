package net.minecraft.world.entity.monster.piglin;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
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

   public AbstractPiglin(EntityType<? extends AbstractPiglin> p_34652_, Level p_34653_) {
      super(p_34652_, p_34653_);
      this.setCanPickUpLoot(true);
      this.applyOpenDoorsAbility();
      this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
   }

   private void applyOpenDoorsAbility() {
      if (GoalUtils.hasGroundPathNavigation(this)) {
         this.getNavigation().setCanOpenDoors(true);
      }
   }

   protected abstract boolean canHunt();

   public void setImmuneToZombification(boolean p_34671_) {
      this.getEntityData().set(DATA_IMMUNE_TO_ZOMBIFICATION, p_34671_);
   }

   protected boolean isImmuneToZombification() {
      return this.getEntityData().get(DATA_IMMUNE_TO_ZOMBIFICATION);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_327959_) {
      super.defineSynchedData(p_327959_);
      p_327959_.define(DATA_IMMUNE_TO_ZOMBIFICATION, false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406258_) {
      super.addAdditionalSaveData(p_406258_);
      p_406258_.putBoolean("IsImmuneToZombification", this.isImmuneToZombification());
      p_406258_.putInt("TimeInOverworld", this.timeInOverworld);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_409004_) {
      super.readAdditionalSaveData(p_409004_);
      this.setCanPickUpLoot(p_409004_.getBooleanOr("CanPickUpLoot", true));
      this.setImmuneToZombification(p_409004_.getBooleanOr("IsImmuneToZombification", false));
      this.timeInOverworld = p_409004_.getIntOr("TimeInOverworld", 0);
   }

   @Override
   protected void customServerAiStep(ServerLevel p_360786_) {
      super.customServerAiStep(p_360786_);
      if (this.isConverting()) {
         this.timeInOverworld++;
      } else {
         this.timeInOverworld = 0;
      }

      if (this.timeInOverworld > 300) {
         this.playConvertedSound();
         this.finishConversion(p_360786_);
      }
   }

   @VisibleForTesting
   public void setTimeInOverworld(int p_367590_) {
      this.timeInOverworld = p_367590_;
   }

   public boolean isConverting() {
      return !this.isImmuneToZombification()
         && !this.isNoAi()
         && this.level().environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, this.position());
   }

   protected void finishConversion(ServerLevel p_34663_) {
      this.convertTo(
         EntityType.ZOMBIFIED_PIGLIN,
         ConversionParams.single(this, true, true),
         p_449701_ -> p_449701_.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0))
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
