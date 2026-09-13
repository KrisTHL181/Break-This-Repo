package net.minecraft.world.entity.monster;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
   protected Monster(final EntityType<? extends Monster> type, final Level level) {
      super(type, level);
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
      float br = this.getLightLevelDependentMagicValue();
      if (br > 0.5F) {
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
   protected SoundEvent getHurtSound(final DamageSource source) {
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
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return -level.getPathfindingCostFromLightLevels(pos);
   }

   public static boolean isDarkEnoughToSpawn(final ServerLevelAccessor level, final BlockPos pos, final RandomSource random) {
      if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
         return false;
      }

      DimensionType dimensionType = level.dimensionType();
      int blockLightLimit = dimensionType.monsterSpawnBlockLightLimit();
      if (blockLightLimit < 15 && level.getBrightness(LightLayer.BLOCK, pos) > blockLightLimit) {
         return false;
      }

      int brightness = level.getLevel().isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
      return brightness <= dimensionType.monsterSpawnLightTest().sample(random);
   }

   public static boolean checkMonsterSpawnRules(
      final EntityType<? extends Mob> type, final ServerLevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return (EntitySpawnReason.ignoresLightRequirements(spawnReason) || isDarkEnoughToSpawn(level, pos, random))
         && checkMobSpawnRules(type, level, spawnReason, pos, random);
   }

   public static boolean checkAnyLightMonsterSpawnRules(
      final EntityType<? extends Monster> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return checkMobSpawnRules(type, level, spawnReason, pos, random);
   }

   public static boolean checkSurfaceMonstersSpawnRules(
      final EntityType<? extends Mob> type, final ServerLevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return checkMonsterSpawnRules(type, level, spawnReason, pos, random) && (EntitySpawnReason.isSpawner(spawnReason) || level.canSeeSky(pos));
   }

   public static AttributeSupplier.Builder createMonsterAttributes() {
      return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);
   }

   @Override
   public boolean shouldDropExperience() {
      return true;
   }

   @Override
   protected boolean shouldDropLoot(final ServerLevel level) {
      return level.getGameRules().get(GameRules.MOB_DROPS);
   }

   public boolean isPreventingPlayerRest(final ServerLevel level, final Player player) {
      return true;
   }

   @Override
   public ItemStack getProjectile(final ItemStack heldWeapon) {
      if (heldWeapon.getItem() instanceof ProjectileWeaponItem) {
         Predicate<ItemStack> supportedProjectiles = ((ProjectileWeaponItem)heldWeapon.getItem()).getSupportedHeldProjectiles();
         ItemStack heldProjectile = ProjectileWeaponItem.getHeldProjectile(this, supportedProjectiles);
         return heldProjectile.isEmpty() ? new ItemStack(Items.ARROW) : heldProjectile;
      } else {
         return ItemStack.EMPTY;
      }
   }
}
