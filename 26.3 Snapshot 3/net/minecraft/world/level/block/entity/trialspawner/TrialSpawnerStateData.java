package net.minecraft.world.level.block.entity.trialspawner;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerStateData {
   private static final String TAG_SPAWN_DATA = "spawn_data";
   private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
   private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
   private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
   final Set<UUID> detectedPlayers = new HashSet<>();
   final Set<UUID> currentMobs = new HashSet<>();
   long cooldownEndsAt;
   long nextMobSpawnsAt;
   int totalMobsSpawned;
   Optional<SpawnData> nextSpawnData = Optional.empty();
   Optional<ResourceKey<LootTable>> ejectingLootTable = Optional.empty();
   private @Nullable Entity displayEntity;
   private @Nullable WeightedList<ItemStack> dispensing;
   double spin;
   double oSpin;

   public TrialSpawnerStateData.Packed pack() {
      return new TrialSpawnerStateData.Packed(
         Set.copyOf(this.detectedPlayers),
         Set.copyOf(this.currentMobs),
         this.cooldownEndsAt,
         this.nextMobSpawnsAt,
         this.totalMobsSpawned,
         this.nextSpawnData,
         this.ejectingLootTable
      );
   }

   public void apply(final TrialSpawnerStateData.Packed packed) {
      this.detectedPlayers.clear();
      this.detectedPlayers.addAll(packed.detectedPlayers);
      this.currentMobs.clear();
      this.currentMobs.addAll(packed.currentMobs);
      this.cooldownEndsAt = packed.cooldownEndsAt;
      this.nextMobSpawnsAt = packed.nextMobSpawnsAt;
      this.totalMobsSpawned = packed.totalMobsSpawned;
      this.nextSpawnData = packed.nextSpawnData;
      this.ejectingLootTable = packed.ejectingLootTable;
   }

   public void reset() {
      this.currentMobs.clear();
      this.nextSpawnData = Optional.empty();
      this.resetStatistics();
   }

   public void resetStatistics() {
      this.detectedPlayers.clear();
      this.totalMobsSpawned = 0;
      this.nextMobSpawnsAt = 0L;
      this.cooldownEndsAt = 0L;
   }

   public boolean hasMobToSpawn(final TrialSpawner trialSpawner, final RandomSource random) {
      boolean hasNextMobToSpawn = this.getOrCreateNextSpawnData(trialSpawner, random).getEntityToSpawn().getString("id").isPresent();
      return hasNextMobToSpawn || !trialSpawner.activeConfig().spawnPotentialsDefinition().isEmpty();
   }

   public boolean hasFinishedSpawningAllMobs(final TrialSpawnerConfig config, final int additionalPlayers) {
      return this.totalMobsSpawned >= config.calculateTargetTotalMobs(additionalPlayers);
   }

   public boolean haveAllCurrentMobsDied() {
      return this.currentMobs.isEmpty();
   }

   public boolean isReadyToSpawnNextMob(final ServerLevel serverLevel, final TrialSpawnerConfig config, final int additionalPlayers) {
      return serverLevel.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < config.calculateTargetSimultaneousMobs(additionalPlayers);
   }

   public int countAdditionalPlayers(final BlockPos pos) {
      if (this.detectedPlayers.isEmpty()) {
         Util.logAndPauseIfInIde("Trial Spawner at " + pos + " has no detected players");
      }

      return Math.max(0, this.detectedPlayers.size() - 1);
   }

   public void tryDetectPlayers(final ServerLevel level, final BlockPos pos, final TrialSpawner trialSpawner) {
      boolean isThrottled = (pos.asLong() + level.getGameTime()) % 20L != 0L;
      if (!isThrottled) {
         if (!trialSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || !trialSpawner.isOminous()) {
            List<UUID> inLineOfSightPlayers = trialSpawner.getPlayerDetector()
               .detect(level, trialSpawner.getEntitySelector(), pos, trialSpawner.getRequiredPlayerRange(), true);
            boolean becameOminous;
            if (!trialSpawner.isOminous() && !inLineOfSightPlayers.isEmpty()) {
               Optional<Pair<Player, Holder<MobEffect>>> playerWithOminousEffect = findPlayerWithOminousEffect(level, inLineOfSightPlayers);
               playerWithOminousEffect.ifPresent(playerAndEffect -> {
                  Player player = (Player)playerAndEffect.getFirst();
                  if (playerAndEffect.getSecond() == MobEffects.BAD_OMEN) {
                     transformBadOmenIntoTrialOmen(player);
                  }

                  level.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
                  trialSpawner.applyOminous(level, pos);
               });
               becameOminous = playerWithOminousEffect.isPresent();
            } else {
               becameOminous = false;
            }

            if (!trialSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || becameOminous) {
               boolean isSearchingForFirstPlayer = trialSpawner.getStateData().detectedPlayers.isEmpty();
               List<UUID> foundPlayers = isSearchingForFirstPlayer
                  ? inLineOfSightPlayers
                  : trialSpawner.getPlayerDetector().detect(level, trialSpawner.getEntitySelector(), pos, trialSpawner.getRequiredPlayerRange(), false);
               if (this.detectedPlayers.addAll(foundPlayers)) {
                  this.nextMobSpawnsAt = Math.max(level.getGameTime() + 40L, this.nextMobSpawnsAt);
                  if (!becameOminous) {
                     int event = trialSpawner.isOminous() ? 3019 : 3013;
                     level.levelEvent(event, pos, this.detectedPlayers.size());
                  }
               }
            }
         }
      }
   }

   private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(final ServerLevel level, final List<UUID> inLineOfSightPlayers) {
      Player playerWithBadOmen = null;

      for (UUID playerUuid : inLineOfSightPlayers) {
         Player player = level.getPlayerByUUID(playerUuid);
         if (player != null) {
            Holder<MobEffect> trialOmen = MobEffects.TRIAL_OMEN;
            if (player.hasEffect(trialOmen)) {
               return Optional.of(Pair.of(player, trialOmen));
            }

            if (player.hasEffect(MobEffects.BAD_OMEN)) {
               playerWithBadOmen = player;
            }
         }
      }

      return Optional.ofNullable(playerWithBadOmen).map(playerx -> Pair.of(playerx, MobEffects.BAD_OMEN));
   }

   public void resetAfterBecomingOminous(final TrialSpawner trialSpawner, final ServerLevel level) {
      this.currentMobs.stream().map(level::getEntity).forEach(entity -> {
         if (entity != null) {
            level.levelEvent(3012, entity.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
            if (entity instanceof Mob mob) {
               mob.dropPreservedEquipment(level);
            }

            entity.remove(Entity.RemovalReason.DISCARDED);
         }
      });
      if (!trialSpawner.ominousConfig().spawnPotentialsDefinition().isEmpty()) {
         this.nextSpawnData = Optional.empty();
      }

      this.totalMobsSpawned = 0;
      this.currentMobs.clear();
      this.nextMobSpawnsAt = level.getGameTime() + trialSpawner.ominousConfig().ticksBetweenSpawn();
      trialSpawner.markUpdated();
      this.cooldownEndsAt = level.getGameTime() + trialSpawner.ominousConfig().ticksBetweenItemSpawners();
   }

   private static void transformBadOmenIntoTrialOmen(final Player player) {
      MobEffectInstance badOmen = player.getEffect(MobEffects.BAD_OMEN);
      if (badOmen != null) {
         int amplifier = badOmen.getAmplifier() + 1;
         int duration = 18000 * amplifier;
         player.removeEffect(MobEffects.BAD_OMEN);
         player.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, duration, 0));
      }
   }

   public boolean isReadyToOpenShutter(final ServerLevel serverLevel, final float delayBeforeOpen, final int targetCooldownLength) {
      long cooldownStartedAt = this.cooldownEndsAt - targetCooldownLength;
      return (float)serverLevel.getGameTime() >= (float)cooldownStartedAt + delayBeforeOpen;
   }

   public boolean isReadyToEjectItems(final ServerLevel serverLevel, final float timeBetweenEjections, final int targetCooldownLength) {
      long cooldownStartedAt = this.cooldownEndsAt - targetCooldownLength;
      return (float)(serverLevel.getGameTime() - cooldownStartedAt) % timeBetweenEjections == 0.0F;
   }

   public boolean isCooldownFinished(final ServerLevel serverLevel) {
      return serverLevel.getGameTime() >= this.cooldownEndsAt;
   }

   protected SpawnData getOrCreateNextSpawnData(final TrialSpawner trialSpawner, final RandomSource random) {
      if (this.nextSpawnData.isPresent()) {
         return this.nextSpawnData.get();
      }

      WeightedList<SpawnData> spawnPotentials = trialSpawner.activeConfig().spawnPotentialsDefinition();
      Optional<SpawnData> selected = spawnPotentials.isEmpty() ? this.nextSpawnData : spawnPotentials.getRandom(random);
      this.nextSpawnData = Optional.of(selected.orElseGet(SpawnData::new));
      trialSpawner.markUpdated();
      return this.nextSpawnData.get();
   }

   public @Nullable Entity getOrCreateDisplayEntity(final TrialSpawner trialSpawner, final Level level, final TrialSpawnerState state) {
      if (!state.hasSpinningMob()) {
         return null;
      }

      if (this.displayEntity == null) {
         CompoundTag entityToSpawn = this.getOrCreateNextSpawnData(trialSpawner, level.getRandom()).getEntityToSpawn();
         if (entityToSpawn.getString("id").isPresent()) {
            this.displayEntity = EntityType.loadEntityRecursive(
               entityToSpawn, level, new EntitySpawnRequest(EntitySpawnReason.TRIAL_SPAWNER, true), BaseSpawner.SET_DISPLAY_ENTITY_ID
            );
         }
      }

      return this.displayEntity;
   }

   public CompoundTag getUpdateTag(final TrialSpawnerState state) {
      CompoundTag tag = new CompoundTag();
      if (state == TrialSpawnerState.ACTIVE) {
         tag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
      }

      this.nextSpawnData.ifPresent(spawnData -> tag.store("spawn_data", SpawnData.CODEC, spawnData));
      return tag;
   }

   public double getSpin() {
      return this.spin;
   }

   public double getOSpin() {
      return this.oSpin;
   }

   public WeightedList<ItemStack> getDispensingItems(final ServerLevel level, final TrialSpawnerConfig config, final BlockPos pos) {
      if (this.dispensing != null) {
         return this.dispensing;
      }

      LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(config.itemsToDropWhenOminous());
      LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
      long simplePositionalSeed = lowResolutionPosition(level, pos);
      ObjectArrayList<ItemStack> lootDrops = lootTable.getRandomItems(params, simplePositionalSeed);
      if (lootDrops.isEmpty()) {
         return WeightedList.of();
      }

      WeightedList.Builder<ItemStack> builder = WeightedList.builder();
      ObjectListIterator var10 = lootDrops.iterator();

      while (var10.hasNext()) {
         ItemStack drop = (ItemStack)var10.next();
         builder.add(drop.copyWithCount(1), drop.getCount());
      }

      this.dispensing = builder.build();
      return this.dispensing;
   }

   private static long lowResolutionPosition(final ServerLevel level, final BlockPos pos) {
      BlockPos lowResolutionPosition = new BlockPos(Mth.floor(pos.getX() / 30.0F), Mth.floor(pos.getY() / 20.0F), Mth.floor(pos.getZ() / 30.0F));
      return level.getSeed() + lowResolutionPosition.asLong();
   }

   public record Packed(
      Set<UUID> detectedPlayers,
      Set<UUID> currentMobs,
      long cooldownEndsAt,
      long nextMobSpawnsAt,
      int totalMobsSpawned,
      Optional<SpawnData> nextSpawnData,
      Optional<ResourceKey<LootTable>> ejectingLootTable
   ) {
      public static final MapCodec<TrialSpawnerStateData.Packed> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Set.of()).forGetter(TrialSpawnerStateData.Packed::detectedPlayers),
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Set.of()).forGetter(TrialSpawnerStateData.Packed::currentMobs),
               Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", 0L).forGetter(TrialSpawnerStateData.Packed::cooldownEndsAt),
               Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", 0L).forGetter(TrialSpawnerStateData.Packed::nextMobSpawnsAt),
               Codec.intRange(0, Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", 0).forGetter(TrialSpawnerStateData.Packed::totalMobsSpawned),
               SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(TrialSpawnerStateData.Packed::nextSpawnData),
               LootTable.KEY_CODEC.lenientOptionalFieldOf("ejecting_loot_table").forGetter(TrialSpawnerStateData.Packed::ejectingLootTable)
            )
            .apply(i, TrialSpawnerStateData.Packed::new)
      );
   }
}
