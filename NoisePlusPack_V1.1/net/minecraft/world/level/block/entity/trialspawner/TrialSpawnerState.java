package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public enum TrialSpawnerState implements StringRepresentable {
   INACTIVE("inactive", 0, TrialSpawnerState.ParticleEmission.NONE, -1.0, false),
   WAITING_FOR_PLAYERS("waiting_for_players", 4, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, 200.0, true),
   ACTIVE("active", 8, TrialSpawnerState.ParticleEmission.FLAMES_AND_SMOKE, 1000.0, true),
   WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
   EJECTING_REWARD("ejecting_reward", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
   COOLDOWN("cooldown", 0, TrialSpawnerState.ParticleEmission.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

   private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0F;
   private static final int TIME_BETWEEN_EACH_EJECTION = Mth.floor(30.0F);
   private final String name;
   private final int lightLevel;
   private final double spinningMobSpeed;
   private final TrialSpawnerState.ParticleEmission particleEmission;
   private final boolean isCapableOfSpawning;

   TrialSpawnerState(
      final String p_309652_, final int p_311553_, final TrialSpawnerState.ParticleEmission p_309474_, final double p_312481_, final boolean p_310488_
   ) {
      this.name = p_309652_;
      this.lightLevel = p_311553_;
      this.particleEmission = p_309474_;
      this.spinningMobSpeed = p_312481_;
      this.isCapableOfSpawning = p_310488_;
   }

   TrialSpawnerState tickAndGetNext(BlockPos p_313024_, TrialSpawner p_310869_, ServerLevel p_313233_) {
      TrialSpawnerStateData trialspawnerstatedata = p_310869_.getStateData();
      TrialSpawnerConfig trialspawnerconfig = p_310869_.activeConfig();

      return switch (this) {
         case INACTIVE -> trialspawnerstatedata.getOrCreateDisplayEntity(p_310869_, p_313233_, WAITING_FOR_PLAYERS) == null ? this : WAITING_FOR_PLAYERS;
         case WAITING_FOR_PLAYERS -> {
            if (!p_310869_.canSpawnInLevel(p_313233_)) {
               trialspawnerstatedata.resetStatistics();
               yield this;
            } else if (!trialspawnerstatedata.hasMobToSpawn(p_310869_, p_313233_.random)) {
               yield INACTIVE;
            } else {
               trialspawnerstatedata.tryDetectPlayers(p_313233_, p_313024_, p_310869_);
               yield trialspawnerstatedata.detectedPlayers.isEmpty() ? this : ACTIVE;
            }
         }
         case ACTIVE -> {
            if (!p_310869_.canSpawnInLevel(p_313233_)) {
               trialspawnerstatedata.resetStatistics();
               yield WAITING_FOR_PLAYERS;
            } else if (!trialspawnerstatedata.hasMobToSpawn(p_310869_, p_313233_.random)) {
               yield INACTIVE;
            } else {
               int i = trialspawnerstatedata.countAdditionalPlayers(p_313024_);
               trialspawnerstatedata.tryDetectPlayers(p_313233_, p_313024_, p_310869_);
               if (p_310869_.isOminous()) {
                  this.spawnOminousOminousItemSpawner(p_313233_, p_313024_, p_310869_);
               }

               if (trialspawnerstatedata.hasFinishedSpawningAllMobs(trialspawnerconfig, i)) {
                  if (trialspawnerstatedata.haveAllCurrentMobsDied()) {
                     trialspawnerstatedata.cooldownEndsAt = p_313233_.getGameTime() + p_310869_.getTargetCooldownLength();
                     trialspawnerstatedata.totalMobsSpawned = 0;
                     trialspawnerstatedata.nextMobSpawnsAt = 0L;
                     yield WAITING_FOR_REWARD_EJECTION;
                  }
               } else if (trialspawnerstatedata.isReadyToSpawnNextMob(p_313233_, trialspawnerconfig, i)) {
                  p_310869_.spawnMob(p_313233_, p_313024_).ifPresent(p_449955_ -> {
                     trialspawnerstatedata.currentMobs.add(p_449955_);
                     trialspawnerstatedata.totalMobsSpawned++;
                     trialspawnerstatedata.nextMobSpawnsAt = p_313233_.getGameTime() + trialspawnerconfig.ticksBetweenSpawn();
                     trialspawnerconfig.spawnPotentialsDefinition().getRandom(p_313233_.getRandom()).ifPresent(p_405745_ -> {
                        trialspawnerstatedata.nextSpawnData = Optional.of(p_405745_);
                        p_310869_.markUpdated();
                     });
                  });
               }

               yield this;
            }
         }
         case WAITING_FOR_REWARD_EJECTION -> {
            if (trialspawnerstatedata.isReadyToOpenShutter(p_313233_, 40.0F, p_310869_.getTargetCooldownLength())) {
               p_313233_.playSound(null, p_313024_, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS);
               yield EJECTING_REWARD;
            } else {
               yield this;
            }
         }
         case EJECTING_REWARD -> {
            if (!trialspawnerstatedata.isReadyToEjectItems(p_313233_, TIME_BETWEEN_EACH_EJECTION, p_310869_.getTargetCooldownLength())) {
               yield this;
            } else if (trialspawnerstatedata.detectedPlayers.isEmpty()) {
               p_313233_.playSound(null, p_313024_, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS);
               trialspawnerstatedata.ejectingLootTable = Optional.empty();
               yield COOLDOWN;
            } else {
               if (trialspawnerstatedata.ejectingLootTable.isEmpty()) {
                  trialspawnerstatedata.ejectingLootTable = trialspawnerconfig.lootTablesToEject().getRandom(p_313233_.getRandom());
               }

               trialspawnerstatedata.ejectingLootTable.ifPresent(p_327391_ -> p_310869_.ejectReward(p_313233_, p_313024_, (ResourceKey<LootTable>)p_327391_));
               trialspawnerstatedata.detectedPlayers.remove(trialspawnerstatedata.detectedPlayers.iterator().next());
               yield this;
            }
         }
         case COOLDOWN -> {
            trialspawnerstatedata.tryDetectPlayers(p_313233_, p_313024_, p_310869_);
            if (!trialspawnerstatedata.detectedPlayers.isEmpty()) {
               trialspawnerstatedata.totalMobsSpawned = 0;
               trialspawnerstatedata.nextMobSpawnsAt = 0L;
               yield ACTIVE;
            } else if (trialspawnerstatedata.isCooldownFinished(p_313233_)) {
               p_310869_.removeOminous(p_313233_, p_313024_);
               trialspawnerstatedata.reset();
               yield WAITING_FOR_PLAYERS;
            } else {
               yield this;
            }
         }
      };
   }

   private void spawnOminousOminousItemSpawner(ServerLevel p_332885_, BlockPos p_332679_, TrialSpawner p_327911_) {
      TrialSpawnerStateData trialspawnerstatedata = p_327911_.getStateData();
      TrialSpawnerConfig trialspawnerconfig = p_327911_.activeConfig();
      ItemStack itemstack = trialspawnerstatedata.getDispensingItems(p_332885_, trialspawnerconfig, p_332679_)
         .getRandom(p_332885_.random)
         .orElse(ItemStack.EMPTY);
      if (!itemstack.isEmpty() && this.timeToSpawnItemSpawner(p_332885_, trialspawnerstatedata)) {
         calculatePositionToSpawnSpawner(p_332885_, p_332679_, p_327911_, trialspawnerstatedata).ifPresent(p_327373_ -> {
            OminousItemSpawner ominousitemspawner = OminousItemSpawner.create(p_332885_, itemstack);
            ominousitemspawner.snapTo(p_327373_);
            p_332885_.addFreshEntity(ominousitemspawner);
            float f = (p_332885_.getRandom().nextFloat() - p_332885_.getRandom().nextFloat()) * 0.2F + 1.0F;
            p_332885_.playSound(null, BlockPos.containing(p_327373_), SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, SoundSource.BLOCKS, 1.0F, f);
            trialspawnerstatedata.cooldownEndsAt = p_332885_.getGameTime() + p_327911_.ominousConfig().ticksBetweenItemSpawners();
         });
      }
   }

   private static Optional<Vec3> calculatePositionToSpawnSpawner(
      ServerLevel p_332378_, BlockPos p_330701_, TrialSpawner p_331338_, TrialSpawnerStateData p_408135_
   ) {
      List<Player> list = p_408135_.detectedPlayers
         .stream()
         .map(p_332378_::getPlayerByUUID)
         .filter(Objects::nonNull)
         .filter(
            p_449958_ -> !p_449958_.isCreative()
               && !p_449958_.isSpectator()
               && p_449958_.isAlive()
               && p_449958_.distanceToSqr(p_330701_.getCenter()) <= Mth.square(p_331338_.getRequiredPlayerRange())
         )
         .toList();
      if (list.isEmpty()) {
         return Optional.empty();
      }

      Entity entity = selectEntityToSpawnItemAbove(list, p_408135_.currentMobs, p_331338_, p_330701_, p_332378_);
      return entity == null ? Optional.empty() : calculatePositionAbove(entity, p_332378_);
   }

   private static Optional<Vec3> calculatePositionAbove(Entity p_332455_, ServerLevel p_334568_) {
      Vec3 vec3 = p_332455_.position();
      Vec3 vec31 = vec3.relative(Direction.UP, p_332455_.getBbHeight() + 2.0F + p_334568_.random.nextInt(4));
      BlockHitResult blockhitresult = p_334568_.clip(new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
      Vec3 vec32 = blockhitresult.getBlockPos().getCenter().relative(Direction.DOWN, 1.0);
      BlockPos blockpos = BlockPos.containing(vec32);
      return !p_334568_.getBlockState(blockpos).getCollisionShape(p_334568_, blockpos).isEmpty() ? Optional.empty() : Optional.of(vec32);
   }

   private static @Nullable Entity selectEntityToSpawnItemAbove(
      List<Player> p_328857_, Set<UUID> p_330482_, TrialSpawner p_335914_, BlockPos p_330933_, ServerLevel p_330297_
   ) {
      Stream<Entity> stream = p_330482_.stream()
         .map(p_330297_::getEntity)
         .filter(Objects::nonNull)
         .filter(p_327381_ -> p_327381_.isAlive() && p_327381_.distanceToSqr(p_330933_.getCenter()) <= Mth.square(p_335914_.getRequiredPlayerRange()));
      List<? extends Entity> list = p_330297_.random.nextBoolean() ? stream.toList() : p_328857_;
      if (list.isEmpty()) {
         return null;
      } else {
         return list.size() == 1 ? list.getFirst() : Util.getRandom(list, p_330297_.random);
      }
   }

   private boolean timeToSpawnItemSpawner(ServerLevel p_332151_, TrialSpawnerStateData p_405924_) {
      return p_332151_.getGameTime() >= p_405924_.cooldownEndsAt;
   }

   public int lightLevel() {
      return this.lightLevel;
   }

   public double spinningMobSpeed() {
      return this.spinningMobSpeed;
   }

   public boolean hasSpinningMob() {
      return this.spinningMobSpeed >= 0.0;
   }

   public boolean isCapableOfSpawning() {
      return this.isCapableOfSpawning;
   }

   public void emitParticles(Level p_310333_, BlockPos p_312414_, boolean p_333242_) {
      this.particleEmission.emit(p_310333_, p_310333_.getRandom(), p_312414_, p_333242_);
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   static class LightLevel {
      private static final int UNLIT = 0;
      private static final int HALF_LIT = 4;
      private static final int LIT = 8;

      private LightLevel() {
      }
   }

   interface ParticleEmission {
      TrialSpawnerState.ParticleEmission NONE = (p_311158_, p_313095_, p_309870_, p_333658_) -> {};
      TrialSpawnerState.ParticleEmission SMALL_FLAMES = (p_327396_, p_327397_, p_327398_, p_327399_) -> {
         if (p_327397_.nextInt(2) == 0) {
            Vec3 vec3 = p_327398_.getCenter().offsetRandom(p_327397_, 0.9F);
            addParticle(p_327399_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME, vec3, p_327396_);
         }
      };
      TrialSpawnerState.ParticleEmission FLAMES_AND_SMOKE = (p_327392_, p_327393_, p_327394_, p_327395_) -> {
         Vec3 vec3 = p_327394_.getCenter().offsetRandom(p_327393_, 1.0F);
         addParticle(ParticleTypes.SMOKE, vec3, p_327392_);
         addParticle(p_327395_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, vec3, p_327392_);
      };
      TrialSpawnerState.ParticleEmission SMOKE_INSIDE_AND_TOP_FACE = (p_312500_, p_312202_, p_311828_, p_334641_) -> {
         Vec3 vec3 = p_311828_.getCenter().offsetRandom(p_312202_, 0.9F);
         if (p_312202_.nextInt(3) == 0) {
            addParticle(ParticleTypes.SMOKE, vec3, p_312500_);
         }

         if (p_312500_.getGameTime() % 20L == 0L) {
            Vec3 vec31 = p_311828_.getCenter().add(0.0, 0.5, 0.0);
            int i = p_312500_.getRandom().nextInt(4) + 20;

            for (int j = 0; j < i; j++) {
               addParticle(ParticleTypes.SMOKE, vec31, p_312500_);
            }
         }
      };

      private static void addParticle(SimpleParticleType p_311275_, Vec3 p_310309_, Level p_310163_) {
         p_310163_.addParticle(p_311275_, p_310309_.x(), p_310309_.y(), p_310309_.z(), 0.0, 0.0, 0.0);
      }

      void emit(Level var1, RandomSource var2, BlockPos var3, boolean var4);
   }

   static class SpinningMob {
      private static final double NONE = -1.0;
      private static final double SLOW = 200.0;
      private static final double FAST = 1000.0;

      private SpinningMob() {
      }
   }
}
