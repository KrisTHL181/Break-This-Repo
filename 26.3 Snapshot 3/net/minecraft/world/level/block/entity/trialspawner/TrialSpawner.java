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
import net.minecraft.world.item.component.TypedEntityData;
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
      final TrialSpawner.FullConfig config,
      final TrialSpawner.StateAccessor stateAccessor,
      final PlayerDetector playerDetector,
      final PlayerDetector.EntitySelector entitySelector
   ) {
      this.config = config;
      this.stateAccessor = stateAccessor;
      this.playerDetector = playerDetector;
      this.entitySelector = entitySelector;
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

   public void load(final ValueInput input) {
      input.<TrialSpawnerStateData.Packed>read(TrialSpawnerStateData.Packed.MAP_CODEC).ifPresent(this.data::apply);
      this.config = input.<TrialSpawner.FullConfig>read(TrialSpawner.FullConfig.MAP_CODEC).orElse(TrialSpawner.FullConfig.DEFAULT);
   }

   public void store(final ValueOutput output) {
      output.store(TrialSpawnerStateData.Packed.MAP_CODEC, this.data.pack());
      output.store(TrialSpawner.FullConfig.MAP_CODEC, this.config);
   }

   public void applyOminous(final ServerLevel level, final BlockPos spawnerPos) {
      level.setBlockAndUpdate(spawnerPos, level.getBlockState(spawnerPos).setValue(TrialSpawnerBlock.OMINOUS, true));
      level.levelEvent(3020, spawnerPos, 1);
      this.isOminous = true;
      this.data.resetAfterBecomingOminous(this, level);
   }

   public void removeOminous(final ServerLevel level, final BlockPos spawnerPos) {
      level.setBlockAndUpdate(spawnerPos, level.getBlockState(spawnerPos).setValue(TrialSpawnerBlock.OMINOUS, false));
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

   public void setState(final Level level, final TrialSpawnerState state) {
      this.stateAccessor.setState(level, state);
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

   public boolean canSpawnInLevel(final ServerLevel level) {
      if (!level.getGameRules().get(GameRules.SPAWNER_BLOCKS_WORK)) {
         return false;
      } else if (this.overridePeacefulAndMobSpawnRule) {
         return true;
      } else {
         return level.getDifficulty() == Difficulty.PEACEFUL ? false : level.getGameRules().get(GameRules.SPAWN_MOBS);
      }
   }

   public Optional<UUID> spawnMob(final ServerLevel level, final BlockPos spawnerPos) {
      RandomSource random = level.getRandom();
      SpawnData nextSpawnData = this.data.getOrCreateNextSpawnData(this, level.getRandom());

      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(() -> "spawner@" + spawnerPos, LOGGER)) {
         ValueInput input = TagValueInput.create(reporter, level.registryAccess(), nextSpawnData.entityToSpawn());
         Optional<EntityType<?>> entityType = EntityType.by(input);
         if (entityType.isEmpty()) {
            return Optional.empty();
         }

         Vec3 spawnPos = input.<Vec3>read("Pos", Vec3.CODEC)
            .orElseGet(
               () -> {
                  TrialSpawnerConfig activeConfig = this.activeConfig();
                  return new Vec3(
                     spawnerPos.getX() + (random.nextDouble() - random.nextDouble()) * activeConfig.spawnRange() + 0.5,
                     spawnerPos.getY() + random.nextInt(3) - 1,
                     spawnerPos.getZ() + (random.nextDouble() - random.nextDouble()) * activeConfig.spawnRange() + 0.5
                  );
               }
            );
         if (!level.noCollision(entityType.get().getSpawnAABB(spawnPos.x, spawnPos.y, spawnPos.z))) {
            return Optional.empty();
         }

         if (!inLineOfSight(level, Vec3.atCenterOf(spawnerPos), spawnPos)) {
            return Optional.empty();
         }

         BlockPos spawnBlockPos = BlockPos.containing(spawnPos);
         if (!SpawnPlacements.checkSpawnRules(entityType.get(), level, EntitySpawnReason.TRIAL_SPAWNER, spawnBlockPos, level.getRandom())) {
            return Optional.empty();
         }

         if (nextSpawnData.getCustomSpawnRules().isPresent()) {
            SpawnData.CustomSpawnRules customSpawnRules = nextSpawnData.getCustomSpawnRules().get();
            if (!customSpawnRules.isValidPosition(spawnBlockPos, level)) {
               return Optional.empty();
            }
         }

         Entity entity = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.TRIAL_SPAWNER, e -> {
            e.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, random.nextFloat() * 360.0F, 0.0F);
            return e;
         });
         if (entity == null) {
            return Optional.empty();
         }

         if (entity instanceof Mob mob) {
            if (!mob.checkSpawnObstruction(level)) {
               return Optional.empty();
            }

            boolean hasNoConfiguration = nextSpawnData.getEntityToSpawn().size() == 1 && nextSpawnData.getEntityToSpawn().getString("id").isPresent();
            if (hasNoConfiguration) {
               mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.TRIAL_SPAWNER, null);
            }

            mob.setPersistenceRequired();
            nextSpawnData.getEquipment().ifPresent(mob::equip);
         }

         if (!level.tryAddFreshEntityWithPassengers(entity)) {
            return Optional.empty();
         }

         TrialSpawner.FlameParticle flameParticle = this.isOminous ? TrialSpawner.FlameParticle.OMINOUS : TrialSpawner.FlameParticle.NORMAL;
         level.levelEvent(3011, spawnerPos, flameParticle.encode());
         level.levelEvent(3012, spawnBlockPos, flameParticle.encode());
         level.gameEvent(entity, GameEvent.ENTITY_PLACE, spawnBlockPos);
         return Optional.of(entity.getUUID());
      }
   }

   public void ejectReward(final ServerLevel level, final BlockPos pos, final ResourceKey<LootTable> ejectingLootTable) {
      LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(ejectingLootTable);
      LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
      ObjectArrayList<ItemStack> lootDrops = lootTable.getRandomItems(params);
      if (!lootDrops.isEmpty()) {
         ObjectListIterator var7 = lootDrops.iterator();

         while (var7.hasNext()) {
            ItemStack item = (ItemStack)var7.next();
            DefaultDispenseItemBehavior.spawnItem(level, item, 2, Direction.UP, Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2));
         }

         level.levelEvent(3014, pos, 0);
      }
   }

   public void tickClient(final Level level, final BlockPos spawnerPos, final boolean isOminous) {
      TrialSpawnerState currentState = this.getState();
      currentState.emitParticles(level, spawnerPos, isOminous);
      if (currentState.hasSpinningMob()) {
         double spawnDelay = Math.max(0L, this.data.nextMobSpawnsAt - level.getGameTime());
         this.data.oSpin = this.data.spin;
         this.data.spin = (this.data.spin + currentState.spinningMobSpeed() / (spawnDelay + 200.0)) % 360.0;
      }

      if (currentState.isCapableOfSpawning()) {
         RandomSource random = level.getRandom();
         if (random.nextFloat() <= 0.02F) {
            SoundEvent ambientSound = isOminous ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
            level.playLocalSound(spawnerPos, ambientSound, SoundSource.BLOCKS, random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F, false);
         }
      }
   }

   public void tickServer(final ServerLevel serverLevel, final BlockPos spawnerPos, final boolean isOminous) {
      this.isOminous = isOminous;
      TrialSpawnerState currentState = this.getState();
      if (this.data.currentMobs.removeIf(id -> shouldMobBeUntracked(serverLevel, spawnerPos, id))) {
         this.data.nextMobSpawnsAt = serverLevel.getGameTime() + this.activeConfig().ticksBetweenSpawn();
      }

      TrialSpawnerState nextState = currentState.tickAndGetNext(spawnerPos, this, serverLevel);
      if (nextState != currentState) {
         this.setState(serverLevel, nextState);
      }
   }

   private static boolean shouldMobBeUntracked(final ServerLevel serverLevel, final BlockPos spawnerPos, final UUID id) {
      Entity entity = serverLevel.getEntity(id);
      return entity == null
         || !entity.isAlive()
         || !entity.level().dimension().equals(serverLevel.dimension())
         || entity.blockPosition().distSqr(spawnerPos) > MAX_MOB_TRACKING_DISTANCE_SQR;
   }

   private static boolean inLineOfSight(final Level level, final Vec3 origin, final Vec3 dest) {
      BlockHitResult hitResult = level.clip(new ClipContext(dest, origin, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
      return hitResult.getBlockPos().equals(BlockPos.containing(origin)) || hitResult.getType() == HitResult.Type.MISS;
   }

   public static void addSpawnParticles(final Level level, final BlockPos pos, final RandomSource random, final SimpleParticleType particleType) {
      for (int i = 0; i < 20; i++) {
         double xP = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
         double yP = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
         double zP = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
         level.addParticle(ParticleTypes.SMOKE, xP, yP, zP, 0.0, 0.0, 0.0);
         level.addParticle(particleType, xP, yP, zP, 0.0, 0.0, 0.0);
      }
   }

   public static void addBecomeOminousParticles(final Level level, final BlockPos pos, final RandomSource random) {
      for (int i = 0; i < 20; i++) {
         double xP = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
         double yP = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
         double zP = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
         double xa = random.nextGaussian() * 0.02;
         double ya = random.nextGaussian() * 0.02;
         double za = random.nextGaussian() * 0.02;
         level.addParticle(ParticleTypes.TRIAL_OMEN, xP, yP, zP, xa, ya, za);
         level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, xP, yP, zP, xa, ya, za);
      }
   }

   public static void addDetectPlayerParticles(final Level level, final BlockPos pos, final RandomSource random, final int data, final ParticleOptions type) {
      for (int i = 0; i < 30 + Math.min(data, 10) * 5; i++) {
         double spreadX = (2.0F * random.nextFloat() - 1.0F) * 0.65;
         double spreadZ = (2.0F * random.nextFloat() - 1.0F) * 0.65;
         double xP = pos.getX() + 0.5 + spreadX;
         double yP = pos.getY() + 0.1 + random.nextFloat() * 0.8;
         double zP = pos.getZ() + 0.5 + spreadZ;
         level.addParticle(type, xP, yP, zP, 0.0, 0.0, 0.0);
      }
   }

   public static void addEjectItemParticles(final Level level, final BlockPos pos, final RandomSource random) {
      for (int i = 0; i < 20; i++) {
         double xp = pos.getX() + 0.4 + random.nextDouble() * 0.2;
         double yp = pos.getY() + 0.4 + random.nextDouble() * 0.2;
         double zp = pos.getZ() + 0.4 + random.nextDouble() * 0.2;
         double xa = random.nextGaussian() * 0.02;
         double ya = random.nextGaussian() * 0.02;
         double za = random.nextGaussian() * 0.02;
         level.addParticle(ParticleTypes.SMALL_FLAME, xp, yp, zp, xa, ya, za * 0.25);
         level.addParticle(ParticleTypes.SMOKE, xp, yp, zp, xa, ya, za);
      }
   }

   public void overrideEntityToSpawn(final EntityType<?> type, final Level level) {
      this.data.reset();
      this.config = this.config.overrideEntity(type);
      this.setState(level, TrialSpawnerState.INACTIVE);
   }

   public void overrideEntityToSpawn(final TypedEntityData<EntityType<?>> entityData, final Level level) {
      this.data.reset();
      this.config = this.config.overrideEntityData(entityData);
      this.setState(level, TrialSpawnerState.INACTIVE);
   }

   @Deprecated(forRemoval = true)
   @VisibleForTesting
   public void setPlayerDetector(final PlayerDetector playerDetector) {
      this.playerDetector = playerDetector;
   }

   @Deprecated(forRemoval = true)
   @VisibleForTesting
   public void overridePeacefulAndMobSpawnRule() {
      this.overridePeacefulAndMobSpawnRule = true;
   }

   public enum FlameParticle {
      NORMAL(ParticleTypes.FLAME),
      OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

      public final SimpleParticleType particleType;

      FlameParticle(final SimpleParticleType particleType) {
         this.particleType = particleType;
      }

      public static TrialSpawner.FlameParticle decode(final int data) {
         TrialSpawner.FlameParticle[] values = values();
         return data <= values.length && data >= 0 ? values[data] : NORMAL;
      }

      public int encode() {
         return this.ordinal();
      }
   }

   public record FullConfig(Holder<TrialSpawnerConfig> normal, Holder<TrialSpawnerConfig> ominous, int targetCooldownLength, int requiredPlayerRange) {
      public static final MapCodec<TrialSpawner.FullConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               TrialSpawnerConfig.CODEC.optionalFieldOf("normal_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(TrialSpawner.FullConfig::normal),
               TrialSpawnerConfig.CODEC
                  .optionalFieldOf("ominous_config", Holder.direct(TrialSpawnerConfig.DEFAULT))
                  .forGetter(TrialSpawner.FullConfig::ominous),
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(TrialSpawner.FullConfig::targetCooldownLength),
               Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(TrialSpawner.FullConfig::requiredPlayerRange)
            )
            .apply(i, TrialSpawner.FullConfig::new)
      );
      public static final TrialSpawner.FullConfig DEFAULT = new TrialSpawner.FullConfig(
         Holder.direct(TrialSpawnerConfig.DEFAULT), Holder.direct(TrialSpawnerConfig.DEFAULT), 36000, 14
      );

      public TrialSpawner.FullConfig overrideEntity(final EntityType<?> type) {
         return new TrialSpawner.FullConfig(
            Holder.direct(this.normal.value().withSpawning(type)),
            Holder.direct(this.ominous.value().withSpawning(type)),
            this.targetCooldownLength,
            this.requiredPlayerRange
         );
      }

      public TrialSpawner.FullConfig overrideEntityData(final TypedEntityData<EntityType<?>> entityData) {
         return new TrialSpawner.FullConfig(
            Holder.direct(this.normal.value().withSpawning(entityData)),
            Holder.direct(this.ominous.value().withSpawning(entityData)),
            this.targetCooldownLength,
            this.requiredPlayerRange
         );
      }
   }

   public interface StateAccessor {
      void setState(Level level, TrialSpawnerState state);

      TrialSpawnerState getState();

      void markUpdated();
   }
}
