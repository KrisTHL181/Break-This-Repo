package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;

public final class TrialSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
   private static final int DEFAULT_TARGET_COOLDOWN_LENGTH = 36000;
   private static final int DEFAULT_PLAYER_SCAN_RANGE = 14;
   private static final int MAX_MOB_TRACKING_DISTANCE = 47;
   private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
   private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02F;
   private final TrialSpawnerStateData data = new TrialSpawnerStateData();
   private TrialSpawner.FullConfig config;
   private final TrialSpawner.StateAccessor stateAccessor;
   private PlayerDetector playerDetector;
   private final PlayerDetector.EntitySelector entitySelector;
   private boolean overridePeacefulAndMobSpawnRule;
   private boolean isOminous;

   public TrialSpawner(
      TrialSpawner.FullConfig p_406041_, TrialSpawner.StateAccessor p_310539_, PlayerDetector p_312974_, PlayerDetector.EntitySelector p_333634_
   ) {
      this.config = p_406041_;
      this.stateAccessor = p_310539_;
      this.playerDetector = p_312974_;
      this.entitySelector = p_333634_;
   }

   public TrialSpawnerConfig activeConfig() {
      return this.isOminous ? this.config.ominous().value() : this.config.normal.value();
   }

   public TrialSpawnerConfig normalConfig() {
      return this.config.normal.value();
   }

   public TrialSpawnerConfig ominousConfig() {
      return this.config.ominous.value();
   }

   public void load(ValueInput p_405864_) {
      p_405864_.<TrialSpawnerStateData.Packed>read(TrialSpawnerStateData.Packed.MAP_CODEC).ifPresent(this.data::apply);
      this.config = p_405864_.<TrialSpawner.FullConfig>read(TrialSpawner.FullConfig.MAP_CODEC).orElse(TrialSpawner.FullConfig.DEFAULT);
   }

   public void store(ValueOutput p_406316_) {
      p_406316_.store(TrialSpawnerStateData.Packed.MAP_CODEC, this.data.pack());
      p_406316_.store(TrialSpawner.FullConfig.MAP_CODEC, this.config);
   }

   public void applyOminous(ServerLevel p_334207_, BlockPos p_327778_) {
      p_334207_.setBlock(p_327778_, p_334207_.getBlockState(p_327778_).setValue(TrialSpawnerBlock.OMINOUS, true), 3);
      p_334207_.levelEvent(3020, p_327778_, 1);
      this.isOminous = true;
      this.data.resetAfterBecomingOminous(this, p_334207_);
   }

   public void removeOminous(ServerLevel p_336080_, BlockPos p_328593_) {
      p_336080_.setBlock(p_328593_, p_336080_.getBlockState(p_328593_).setValue(TrialSpawnerBlock.OMINOUS, false), 3);
      this.isOminous = false;
   }

   public boolean isOminous() {
      return this.isOminous;
   }

   public int getTargetCooldownLength() {
      return this.config.targetCooldownLength;
   }

   public int getRequiredPlayerRange() {
      return this.config.requiredPlayerRange;
   }

   public TrialSpawnerState getState() {
      return this.stateAccessor.getState();
   }

   public TrialSpawnerStateData getStateData() {
      return this.data;
   }

   public void setState(Level p_310153_, TrialSpawnerState p_312484_) {
      this.stateAccessor.setState(p_310153_, p_312484_);
   }

   public void markUpdated() {
      this.stateAccessor.markUpdated();
   }

   public PlayerDetector getPlayerDetector() {
      return this.playerDetector;
   }

   public PlayerDetector.EntitySelector getEntitySelector() {
      return this.entitySelector;
   }

   public boolean canSpawnInLevel(ServerLevel p_361781_) {
      if (!p_361781_.getGameRules().get(GameRules.SPAWNER_BLOCKS_WORK)) {
         return false;
      } else if (this.overridePeacefulAndMobSpawnRule) {
         return true;
      } else {
         return p_361781_.getDifficulty() == Difficulty.PEACEFUL ? false : p_361781_.getGameRules().get(GameRules.SPAWN_MOBS);
      }
   }

   public Optional<UUID> spawnMob(ServerLevel p_312690_, BlockPos p_313108_) {
      RandomSource randomsource = p_312690_.getRandom();
      SpawnData spawndata = this.data.getOrCreateNextSpawnData(this, p_312690_.getRandom());

      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(() -> "spawner@" + p_313108_, LOGGER)) {
         ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, p_312690_.registryAccess(), spawndata.entityToSpawn());
         Optional<EntityType<?>> optional = EntityType.by(valueinput);
         if (optional.isEmpty()) {
            return Optional.empty();
         }

         Vec3 vec3 = valueinput.<Vec3>read("Pos", Vec3.CODEC)
            .orElseGet(
               () -> {
                  TrialSpawnerConfig trialspawnerconfig = this.activeConfig();
                  return new Vec3(
                     p_313108_.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * trialspawnerconfig.spawnRange() + 0.5,
                     p_313108_.getY() + randomsource.nextInt(3) - 1,
                     p_313108_.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * trialspawnerconfig.spawnRange() + 0.5
                  );
               }
            );
         if (!p_312690_.noCollision(optional.get().getSpawnAABB(vec3.x, vec3.y, vec3.z))) {
            return Optional.empty();
         }

         if (!inLineOfSight(p_312690_, p_313108_.getCenter(), vec3)) {
            return Optional.empty();
         }

         BlockPos blockpos = BlockPos.containing(vec3);
         if (!SpawnPlacements.checkSpawnRules(optional.get(), p_312690_, EntitySpawnReason.TRIAL_SPAWNER, blockpos, p_312690_.getRandom())) {
            return Optional.empty();
         }

         if (spawndata.getCustomSpawnRules().isPresent()) {
            SpawnData.CustomSpawnRules spawndata$customspawnrules = spawndata.getCustomSpawnRules().get();
            if (!spawndata$customspawnrules.isValidPosition(blockpos, p_312690_)) {
               return Optional.empty();
            }
         }

         Entity entity = EntityType.loadEntityRecursive(valueinput, p_312690_, EntitySpawnReason.TRIAL_SPAWNER, p_390986_ -> {
            p_390986_.snapTo(vec3.x, vec3.y, vec3.z, randomsource.nextFloat() * 360.0F, 0.0F);
            return p_390986_;
         });
         if (entity == null) {
            return Optional.empty();
         }

         if (entity instanceof Mob mob) {
            if (!mob.checkSpawnObstruction(p_312690_)) {
               return Optional.empty();
            }

            boolean flag = spawndata.getEntityToSpawn().size() == 1 && spawndata.getEntityToSpawn().getString("id").isPresent();
            if (flag) {
               mob.finalizeSpawn(p_312690_, p_312690_.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.TRIAL_SPAWNER, null);
            }

            mob.setPersistenceRequired();
            spawndata.getEquipment().ifPresent(mob::equip);
         }

         if (!p_312690_.tryAddFreshEntityWithPassengers(entity)) {
            return Optional.empty();
         }

         TrialSpawner.FlameParticle trialspawner$flameparticle = this.isOminous ? TrialSpawner.FlameParticle.OMINOUS : TrialSpawner.FlameParticle.NORMAL;
         p_312690_.levelEvent(3011, p_313108_, trialspawner$flameparticle.encode());
         p_312690_.levelEvent(3012, blockpos, trialspawner$flameparticle.encode());
         p_312690_.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
         return Optional.of(entity.getUUID());
      }
   }

   public void ejectReward(ServerLevel p_310080_, BlockPos p_311547_, ResourceKey<LootTable> p_330647_) {
      LootTable loottable = p_310080_.getServer().reloadableRegistries().getLootTable(p_330647_);
      LootParams lootparams = new LootParams.Builder(p_310080_).create(LootContextParamSets.EMPTY);
      ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams);
      if (!objectarraylist.isEmpty()) {
         ObjectListIterator var7 = objectarraylist.iterator();

         while (var7.hasNext()) {
            ItemStack itemstack = (ItemStack)var7.next();
            DefaultDispenseItemBehavior.spawnItem(p_310080_, itemstack, 2, Direction.UP, Vec3.atBottomCenterOf(p_311547_).relative(Direction.UP, 1.2));
         }

         p_310080_.levelEvent(3014, p_311547_, 0);
      }
   }

   public void tickClient(Level p_309627_, BlockPos p_311485_, boolean p_332221_) {
      TrialSpawnerState trialspawnerstate = this.getState();
      trialspawnerstate.emitParticles(p_309627_, p_311485_, p_332221_);
      if (trialspawnerstate.hasSpinningMob()) {
         double d0 = Math.max(0L, this.data.nextMobSpawnsAt - p_309627_.getGameTime());
         this.data.oSpin = this.data.spin;
         this.data.spin = (this.data.spin + trialspawnerstate.spinningMobSpeed() / (d0 + 200.0)) % 360.0;
      }

      if (trialspawnerstate.isCapableOfSpawning()) {
         RandomSource randomsource = p_309627_.getRandom();
         if (randomsource.nextFloat() <= 0.02F) {
            SoundEvent soundevent = p_332221_ ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
            p_309627_.playLocalSound(
               p_311485_, soundevent, SoundSource.BLOCKS, randomsource.nextFloat() * 0.25F + 0.75F, randomsource.nextFloat() + 0.5F, false
            );
         }
      }
   }

   public void tickServer(ServerLevel p_310996_, BlockPos p_312836_, boolean p_332881_) {
      this.isOminous = p_332881_;
      TrialSpawnerState trialspawnerstate = this.getState();
      if (this.data.currentMobs.removeIf(p_309715_ -> shouldMobBeUntracked(p_310996_, p_312836_, p_309715_))) {
         this.data.nextMobSpawnsAt = p_310996_.getGameTime() + this.activeConfig().ticksBetweenSpawn();
      }

      TrialSpawnerState trialspawnerstate1 = trialspawnerstate.tickAndGetNext(p_312836_, this, p_310996_);
      if (trialspawnerstate1 != trialspawnerstate) {
         this.setState(p_310996_, trialspawnerstate1);
      }
   }

   private static boolean shouldMobBeUntracked(ServerLevel p_312275_, BlockPos p_310158_, UUID p_312011_) {
      Entity entity = p_312275_.getEntity(p_312011_);
      return entity == null
         || !entity.isAlive()
         || !entity.level().dimension().equals(p_312275_.dimension())
         || entity.blockPosition().distSqr(p_310158_) > MAX_MOB_TRACKING_DISTANCE_SQR;
   }

   private static boolean inLineOfSight(Level p_311873_, Vec3 p_311845_, Vec3 p_312229_) {
      BlockHitResult blockhitresult = p_311873_.clip(
         new ClipContext(p_312229_, p_311845_, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())
      );
      return blockhitresult.getBlockPos().equals(BlockPos.containing(p_311845_)) || blockhitresult.getType() == HitResult.Type.MISS;
   }

   public static void addSpawnParticles(Level p_333032_, BlockPos p_328008_, RandomSource p_330922_, SimpleParticleType p_331431_) {
      for (int i = 0; i < 20; i++) {
         double d0 = p_328008_.getX() + 0.5 + (p_330922_.nextDouble() - 0.5) * 2.0;
         double d1 = p_328008_.getY() + 0.5 + (p_330922_.nextDouble() - 0.5) * 2.0;
         double d2 = p_328008_.getZ() + 0.5 + (p_330922_.nextDouble() - 0.5) * 2.0;
         p_333032_.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
         p_333032_.addParticle(p_331431_, d0, d1, d2, 0.0, 0.0, 0.0);
      }
   }

   public static void addBecomeOminousParticles(Level p_312837_, BlockPos p_311261_, RandomSource p_312356_) {
      for (int i = 0; i < 20; i++) {
         double d0 = p_311261_.getX() + 0.5 + (p_312356_.nextDouble() - 0.5) * 2.0;
         double d1 = p_311261_.getY() + 0.5 + (p_312356_.nextDouble() - 0.5) * 2.0;
         double d2 = p_311261_.getZ() + 0.5 + (p_312356_.nextDouble() - 0.5) * 2.0;
         double d3 = p_312356_.nextGaussian() * 0.02;
         double d4 = p_312356_.nextGaussian() * 0.02;
         double d5 = p_312356_.nextGaussian() * 0.02;
         p_312837_.addParticle(ParticleTypes.TRIAL_OMEN, d0, d1, d2, d3, d4, d5);
         p_312837_.addParticle(ParticleTypes.SOUL_FIRE_FLAME, d0, d1, d2, d3, d4, d5);
      }
   }

   public static void addDetectPlayerParticles(Level p_309415_, BlockPos p_309941_, RandomSource p_310263_, int p_310988_, ParticleOptions p_331085_) {
      for (int i = 0; i < 30 + Math.min(p_310988_, 10) * 5; i++) {
         double d0 = (2.0F * p_310263_.nextFloat() - 1.0F) * 0.65;
         double d1 = (2.0F * p_310263_.nextFloat() - 1.0F) * 0.65;
         double d2 = p_309941_.getX() + 0.5 + d0;
         double d3 = p_309941_.getY() + 0.1 + p_310263_.nextFloat() * 0.8;
         double d4 = p_309941_.getZ() + 0.5 + d1;
         p_309415_.addParticle(p_331085_, d2, d3, d4, 0.0, 0.0, 0.0);
      }
   }

   public static void addEjectItemParticles(Level p_311170_, BlockPos p_309958_, RandomSource p_309409_) {
      for (int i = 0; i < 20; i++) {
         double d0 = p_309958_.getX() + 0.4 + p_309409_.nextDouble() * 0.2;
         double d1 = p_309958_.getY() + 0.4 + p_309409_.nextDouble() * 0.2;
         double d2 = p_309958_.getZ() + 0.4 + p_309409_.nextDouble() * 0.2;
         double d3 = p_309409_.nextGaussian() * 0.02;
         double d4 = p_309409_.nextGaussian() * 0.02;
         double d5 = p_309409_.nextGaussian() * 0.02;
         p_311170_.addParticle(ParticleTypes.SMALL_FLAME, d0, d1, d2, d3, d4, d5 * 0.25);
         p_311170_.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
      }
   }

   public void overrideEntityToSpawn(EntityType<?> p_378280_, Level p_376000_) {
      this.data.reset();
      this.config = this.config.overrideEntity(p_378280_);
      this.setState(p_376000_, TrialSpawnerState.INACTIVE);
   }

   @VisibleForTesting
   public void setPlayerDetector(PlayerDetector p_311472_) {
      this.playerDetector = p_311472_;
   }

   @VisibleForTesting
   public void overridePeacefulAndMobSpawnRule() {
      this.overridePeacefulAndMobSpawnRule = true;
   }

   public enum FlameParticle {
      NORMAL(ParticleTypes.FLAME),
      OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

      public final SimpleParticleType particleType;

      FlameParticle(final SimpleParticleType p_332977_) {
         this.particleType = p_332977_;
      }

      public static TrialSpawner.FlameParticle decode(int p_333274_) {
         TrialSpawner.FlameParticle[] atrialspawner$flameparticle = values();
         return p_333274_ <= atrialspawner$flameparticle.length && p_333274_ >= 0 ? atrialspawner$flameparticle[p_333274_] : NORMAL;
      }

      public int encode() {
         return this.ordinal();
      }
   }

   public record FullConfig(Holder<TrialSpawnerConfig> normal, Holder<TrialSpawnerConfig> ominous, int targetCooldownLength, int requiredPlayerRange) {
      public static final MapCodec<TrialSpawner.FullConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_408674_ -> p_408674_.group(
               TrialSpawnerConfig.CODEC.optionalFieldOf("normal_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(TrialSpawner.FullConfig::normal),
               TrialSpawnerConfig.CODEC
                  .optionalFieldOf("ominous_config", Holder.direct(TrialSpawnerConfig.DEFAULT))
                  .forGetter(TrialSpawner.FullConfig::ominous),
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(TrialSpawner.FullConfig::targetCooldownLength),
               Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(TrialSpawner.FullConfig::requiredPlayerRange)
            )
            .apply(p_408674_, TrialSpawner.FullConfig::new)
      );
      public static final TrialSpawner.FullConfig DEFAULT = new TrialSpawner.FullConfig(
         Holder.direct(TrialSpawnerConfig.DEFAULT), Holder.direct(TrialSpawnerConfig.DEFAULT), 36000, 14
      );

      public TrialSpawner.FullConfig overrideEntity(EntityType<?> p_408619_) {
         return new TrialSpawner.FullConfig(
            Holder.direct(this.normal.value().withSpawning(p_408619_)),
            Holder.direct(this.ominous.value().withSpawning(p_408619_)),
            this.targetCooldownLength,
            this.requiredPlayerRange
         );
      }
   }

   public interface StateAccessor {
      void setState(Level var1, TrialSpawnerState var2);

      TrialSpawnerState getState();

      void markUpdated();
   }
}
