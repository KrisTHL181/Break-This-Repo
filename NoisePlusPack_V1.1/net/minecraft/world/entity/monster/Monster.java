package net.minecraft.world.entity.monster;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;

public abstract class Monster extends PathfinderMob implements Enemy {
   protected Monster(EntityType<? extends Monster> p_33002_, Level p_33003_) {
      super(p_33002_, p_33003_);
      this.xpReward = 5;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   public void aiStep() {
      this.updateSwingTime();
      this.updateNoActionTime();
      super.aiStep();
   }

   protected void updateNoActionTime() {
      float f = this.getLightLevelDependentMagicValue();
      if (f > 0.5F) {
         this.noActionTime += 2;
      }
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.HOSTILE_SWIM;
   }

   @Override
   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.HOSTILE_SPLASH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_33034_) {
      return SoundEvents.HOSTILE_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.HOSTILE_DEATH;
   }

   @Override
   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.HOSTILE_SMALL_FALL, SoundEvents.HOSTILE_BIG_FALL);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_33013_, LevelReader p_33014_) {
      return -p_33014_.getPathfindingCostFromLightLevels(p_33013_);
   }

   public static boolean isDarkEnoughToSpawn(ServerLevelAccessor p_219010_, BlockPos p_219011_, RandomSource p_219012_) {
      if (p_219010_.getBrightness(LightLayer.SKY, p_219011_) > p_219012_.nextInt(32)) {
         return false;
      }

      DimensionType dimensiontype = p_219010_.dimensionType();
      int i = dimensiontype.monsterSpawnBlockLightLimit();
      if (i < 15 && p_219010_.getBrightness(LightLayer.BLOCK, p_219011_) > i) {
         return false;
      }

      int j = p_219010_.getLevel().isThundering() ? p_219010_.getMaxLocalRawBrightness(p_219011_, 10) : p_219010_.getMaxLocalRawBrightness(p_219011_);
      return j <= dimensiontype.monsterSpawnLightTest().sample(p_219012_);
   }

   public static boolean checkMonsterSpawnRules(
      EntityType<? extends Mob> p_219014_, ServerLevelAccessor p_219015_, EntitySpawnReason p_361279_, BlockPos p_219017_, RandomSource p_219018_
   ) {
      return p_219015_.getDifficulty() != Difficulty.PEACEFUL
         && (EntitySpawnReason.ignoresLightRequirements(p_361279_) || isDarkEnoughToSpawn(p_219015_, p_219017_, p_219018_))
         && checkMobSpawnRules(p_219014_, p_219015_, p_361279_, p_219017_, p_219018_);
   }

   public static boolean checkAnyLightMonsterSpawnRules(
      EntityType<? extends Monster> p_219020_, LevelAccessor p_219021_, EntitySpawnReason p_362154_, BlockPos p_219023_, RandomSource p_219024_
   ) {
      return p_219021_.getDifficulty() != Difficulty.PEACEFUL && checkMobSpawnRules(p_219020_, p_219021_, p_362154_, p_219023_, p_219024_);
   }

   public static boolean checkSurfaceMonstersSpawnRules(
      EntityType<? extends Mob> p_459348_, ServerLevelAccessor p_455072_, EntitySpawnReason p_450464_, BlockPos p_458027_, RandomSource p_458828_
   ) {
      return checkMonsterSpawnRules(p_459348_, p_455072_, p_450464_, p_458027_, p_458828_)
         && (EntitySpawnReason.isSpawner(p_450464_) || p_455072_.canSeeSky(p_458027_));
   }

   public static AttributeSupplier.Builder createMonsterAttributes() {
      return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);
   }

   @Override
   public boolean shouldDropExperience() {
      return true;
   }

   @Override
   protected boolean shouldDropLoot(ServerLevel p_431253_) {
      return p_431253_.getGameRules().get(GameRules.MOB_DROPS);
   }

   public boolean isPreventingPlayerRest(ServerLevel p_369968_, Player p_33036_) {
      return true;
   }

   @Override
   public ItemStack getProjectile(ItemStack p_33038_) {
      if (p_33038_.getItem() instanceof ProjectileWeaponItem) {
         Predicate<ItemStack> predicate = ((ProjectileWeaponItem)p_33038_.getItem()).getSupportedHeldProjectiles();
         ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
         return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
      } else {
         return ItemStack.EMPTY;
      }
   }
}
