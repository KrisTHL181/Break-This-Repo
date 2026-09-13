package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PiglinBrute extends AbstractPiglin {
   private static final int MAX_HEALTH = 50;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.35F;
   private static final int ATTACK_DAMAGE = 7;
   private static final double TARGETING_RANGE = 12.0;
   protected static final ImmutableList<SensorType<? extends Sensor<? super PiglinBrute>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.HURT_BY, SensorType.PIGLIN_BRUTE_SPECIFIC_SENSOR
   );
   protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.DOORS_TO_CLOSE,
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
      MemoryModuleType.NEARBY_ADULT_PIGLINS,
      MemoryModuleType.HURT_BY,
      MemoryModuleType.HURT_BY_ENTITY,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      new MemoryModuleType[]{
         MemoryModuleType.ATTACK_TARGET,
         MemoryModuleType.ATTACK_COOLING_DOWN,
         MemoryModuleType.INTERACTION_TARGET,
         MemoryModuleType.PATH,
         MemoryModuleType.ANGRY_AT,
         MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
         MemoryModuleType.HOME
      }
   );

   public PiglinBrute(EntityType<? extends PiglinBrute> p_35048_, Level p_35049_) {
      super(p_35048_, p_35049_);
      this.xpReward = 20;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 50.0)
         .add(Attributes.MOVEMENT_SPEED, 0.35F)
         .add(Attributes.ATTACK_DAMAGE, 7.0)
         .add(Attributes.FOLLOW_RANGE, 12.0);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_35058_, DifficultyInstance p_35059_, EntitySpawnReason p_368605_, @Nullable SpawnGroupData p_35061_
   ) {
      PiglinBruteAi.initMemories(this);
      this.populateDefaultEquipmentSlots(p_35058_.getRandom(), p_35059_);
      return super.finalizeSpawn(p_35058_, p_35059_, p_368605_, p_35061_);
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_219209_, DifficultyInstance p_219210_) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
   }

   @Override
   protected Brain.Provider<PiglinBrute> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_35064_) {
      return PiglinBruteAi.makeBrain(this, this.brainProvider().makeBrain(p_35064_));
   }

   @Override
   public Brain<PiglinBrute> getBrain() {
      return (Brain<PiglinBrute>)super.getBrain();
   }

   @Override
   public boolean canHunt() {
      return false;
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_362289_, ItemStack p_35078_) {
      return p_35078_.is(Items.GOLDEN_AXE) ? super.wantsToPickUp(p_362289_, p_35078_) : false;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_368941_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("piglinBruteBrain");
      this.getBrain().tick(p_368941_, this);
      profilerfiller.pop();
      PiglinBruteAi.updateActivity(this);
      PiglinBruteAi.maybePlayActivitySound(this);
      super.customServerAiStep(p_368941_);
   }

   @Override
   public PiglinArmPose getArmPose() {
      return this.isAggressive() && this.isHoldingMeleeWeapon() ? PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON : PiglinArmPose.DEFAULT;
   }

   @Override
   public boolean hurtServer(ServerLevel p_369105_, DamageSource p_363308_, float p_366796_) {
      boolean flag = super.hurtServer(p_369105_, p_363308_, p_366796_);
      if (flag && p_363308_.getEntity() instanceof LivingEntity livingentity) {
         PiglinBruteAi.wasHurtBy(p_369105_, this, livingentity);
      }

      return flag;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PIGLIN_BRUTE_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_35072_) {
      return SoundEvents.PIGLIN_BRUTE_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PIGLIN_BRUTE_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_35066_, BlockState p_35067_) {
      this.playSound(SoundEvents.PIGLIN_BRUTE_STEP, 0.15F, 1.0F);
   }

   protected void playAngrySound() {
      this.makeSound(SoundEvents.PIGLIN_BRUTE_ANGRY);
   }

   @Override
   protected void playConvertedSound() {
      this.makeSound(SoundEvents.PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED);
   }
}
