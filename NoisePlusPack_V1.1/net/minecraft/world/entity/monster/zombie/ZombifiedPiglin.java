package net.minecraft.world.entity.monster.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class ZombifiedPiglin extends Zombie implements NeutralMob {
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.ZOMBIFIED_PIGLIN.getDimensions().scale(0.5F).withEyeHeight(0.97F);
   private static final Identifier SPEED_MODIFIER_ATTACKING_ID = Identifier.withDefaultNamespace("attacking");
   private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(
      SPEED_MODIFIER_ATTACKING_ID, 0.05, AttributeModifier.Operation.ADD_VALUE
   );
   private static final UniformInt FIRST_ANGER_SOUND_DELAY = TimeUtil.rangeOfSeconds(0, 1);
   private int playFirstAngerSoundIn;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private long persistentAngerEndTime;
   private @Nullable EntityReference<LivingEntity> persistentAngerTarget;
   private static final int ALERT_RANGE_Y = 10;
   private static final UniformInt ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);
   private int ticksUntilNextAlert;

   public ZombifiedPiglin(EntityType<? extends ZombifiedPiglin> p_457284_, Level p_451957_) {
      super(p_457284_, p_451957_);
      this.setPathfindingMalus(PathType.LAVA, 8.0F);
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(1, new SpearUseGoal<>(this, 1.0, 1.0, 10.0F, 2.0F));
      this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
      this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Zombie.createAttributes()
         .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
         .add(Attributes.MOVEMENT_SPEED, 0.23F)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_458920_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_458920_);
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_459919_) {
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (this.isAngry()) {
         if (!this.isBaby() && !attributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING_ID)) {
            attributeinstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
         }

         this.maybePlayFirstAngerSound();
      } else if (attributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING_ID)) {
         attributeinstance.removeModifier(SPEED_MODIFIER_ATTACKING_ID);
      }

      this.updatePersistentAnger(p_459919_, true);
      if (this.getTarget() != null) {
         this.maybeAlertOthers();
      }

      super.customServerAiStep(p_459919_);
   }

   private void maybePlayFirstAngerSound() {
      if (this.playFirstAngerSoundIn > 0) {
         this.playFirstAngerSoundIn--;
         if (this.playFirstAngerSoundIn == 0) {
            this.playAngerSound();
         }
      }
   }

   private void maybeAlertOthers() {
      if (this.ticksUntilNextAlert > 0) {
         this.ticksUntilNextAlert--;
      } else {
         if (this.getSensing().hasLineOfSight(this.getTarget())) {
            this.alertOthers();
         }

         this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
      }
   }

   private void alertOthers() {
      double d0 = this.getAttributeValue(Attributes.FOLLOW_RANGE);
      AABB aabb = AABB.unitCubeFromLowerCorner(this.position()).inflate(d0, 10.0, d0);
      this.level()
         .getEntitiesOfClass(ZombifiedPiglin.class, aabb, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(p_451301_ -> p_451301_ != this)
         .filter(p_458817_ -> p_458817_.getTarget() == null)
         .filter(p_457125_ -> !p_457125_.isAlliedTo(this.getTarget()))
         .forEach(p_454260_ -> p_454260_.setTarget(this.getTarget()));
   }

   private void playAngerSound() {
      this.playSound(SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, this.getSoundVolume() * 2.0F, this.getVoicePitch() * 1.8F);
   }

   @Override
   public void setTarget(@Nullable LivingEntity p_460470_) {
      if (this.getTarget() == null && p_460470_ != null) {
         this.playFirstAngerSoundIn = FIRST_ANGER_SOUND_DELAY.sample(this.random);
         this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
      }

      super.setTarget(p_460470_);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   public static boolean checkZombifiedPiglinSpawnRules(
      EntityType<ZombifiedPiglin> p_457443_, LevelAccessor p_456050_, EntitySpawnReason p_454219_, BlockPos p_457562_, RandomSource p_454921_
   ) {
      return p_456050_.getDifficulty() != Difficulty.PEACEFUL && !p_456050_.getBlockState(p_457562_.below()).is(Blocks.NETHER_WART_BLOCK);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_455957_) {
      return p_455957_.isUnobstructed(this) && !p_455957_.containsAnyLiquid(this.getBoundingBox());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_454964_) {
      super.addAdditionalSaveData(p_454964_);
      this.addPersistentAngerSaveData(p_454964_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_455212_) {
      super.readAdditionalSaveData(p_455212_);
      this.readPersistentAngerSaveData(this.level(), p_455212_);
   }

   @Override
   public void setPersistentAngerEndTime(long p_453892_) {
      this.persistentAngerEndTime = p_453892_;
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.persistentAngerEndTime;
   }

   @Override
   public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> p_453838_) {
      this.persistentAngerTarget = p_453838_;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isAngry() ? SoundEvents.ZOMBIFIED_PIGLIN_ANGRY : SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_460703_) {
      return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
   }

   @Override
   public void populateDefaultEquipmentSlots(RandomSource p_459606_, DifficultyInstance p_452196_) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(p_459606_.nextInt(20) == 0 ? Items.GOLDEN_SPEAR : Items.GOLDEN_SWORD));
   }

   @Override
   protected void randomizeReinforcementsChance() {
      this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   @Override
   public boolean isPreventingPlayerRest(ServerLevel p_450742_, Player p_459287_) {
      return this.isAngryAt(p_459287_, p_450742_);
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_453151_, ItemStack p_452035_) {
      return this.canHoldItem(p_452035_);
   }
}
