package net.minecraft.world.entity.monster.cubemob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.jspecify.annotations.Nullable;

public class Slime extends AbstractCubeMob implements Enemy {
   public Slime(final EntityType<? extends Slime> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(2, new AbstractCubeMob.CubeMobAttackGoal(this));
   }

   @Override
   protected void addTargetingGoals() {
      this.targetSelector
         .addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> Math.abs(target.getY() - this.getY()) <= 4.0));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return this.isTiny() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isTiny() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
   }

   @Override
   protected ParticleOptions getParticleType() {
      return ParticleTypes.ITEM_SLIME;
   }

   @Override
   protected SoundEvent getSquishSound() {
      return this.isTiny() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
   }

   @Override
   protected SoundEvent getJumpSound() {
      return this.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
   }

   public static boolean checkSlimeSpawnRules(
      final EntityType<Slime> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      if (level.getDifficulty() != Difficulty.PEACEFUL) {
         if (EntitySpawnReason.isSpawner(spawnReason)) {
            return checkMobSpawnRules(type, level, spawnReason, pos, random);
         }

         if (level.getBiome(pos).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS) && pos.getY() > 50 && pos.getY() < 70) {
            float surfaceSlimeSpawnChance = level.environmentAttributes().getValue(EnvironmentAttributes.SURFACE_SLIME_SPAWN_CHANCE, pos);
            if (random.nextFloat() < surfaceSlimeSpawnChance && level.getMaxLocalRawBrightness(pos) <= random.nextInt(8)) {
               return checkMobSpawnRules(type, level, spawnReason, pos, random);
            }
         }

         if (!(level instanceof WorldGenLevel worldGenLevel)) {
            return false;
         }

         ChunkPos chunkPos = ChunkPos.containing(pos);
         boolean slimeChunk = WorldgenRandom.seedSlimeChunk(chunkPos.x(), chunkPos.z(), worldGenLevel.getSeed(), 987234911L).nextInt(10) == 0;
         if (random.nextInt(10) == 0 && slimeChunk && pos.getY() < 40) {
            return checkMobSpawnRules(type, level, spawnReason, pos, random);
         }
      }

      return false;
   }

   @Override
   protected boolean canBeABaby() {
      return false;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      if (groupData == null) {
         groupData = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   public void setSize(final int size, final boolean updateHealth) {
      super.setSize(size, updateHealth);
      int actualSize = this.entityData.get(ID_SIZE);
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(actualSize);
      this.xpReward = actualSize;
   }
}
