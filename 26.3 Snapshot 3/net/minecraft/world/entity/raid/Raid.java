package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Raid {
   public static final SpawnPlacementType RAVAGER_SPAWN_PLACEMENT_TYPE = SpawnPlacements.getPlacementType(EntityTypes.RAVAGER);
   public static final MapCodec<Raid> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.BOOL.fieldOf("started").forGetter(r -> r.started),
            Codec.BOOL.fieldOf("active").forGetter(r -> r.active),
            Codec.LONG.fieldOf("ticks_active").forGetter(r -> r.ticksActive),
            Codec.INT.fieldOf("raid_omen_level").forGetter(r -> r.raidOmenLevel),
            Codec.INT.fieldOf("groups_spawned").forGetter(r -> r.groupsSpawned),
            Codec.INT.fieldOf("cooldown_ticks").forGetter(r -> r.raidCooldownTicks),
            Codec.INT.fieldOf("post_raid_ticks").forGetter(r -> r.postRaidTicks),
            Codec.FLOAT.fieldOf("total_health").forGetter(r -> r.totalHealth),
            Codec.INT.fieldOf("group_count").forGetter(r -> r.numGroups),
            Raid.RaidStatus.CODEC.fieldOf("status").forGetter(r -> r.status),
            BlockPos.CODEC.fieldOf("center").forGetter(r -> r.center),
            UUIDUtil.CODEC_SET.fieldOf("heroes_of_the_village").forGetter(r -> r.heroesOfTheVillage)
         )
         .apply(i, Raid::new)
   );
   private static final int ALLOW_SPAWNING_WITHIN_VILLAGE_SECONDS_THRESHOLD = 7;
   private static final int SECTION_RADIUS_FOR_FINDING_NEW_VILLAGE_CENTER = 2;
   private static final int VILLAGE_SEARCH_RADIUS = 32;
   private static final int RAID_TIMEOUT_TICKS = 48000;
   private static final int NUM_SPAWN_ATTEMPTS = 5;
   private static final Component OMINOUS_BANNER_PATTERN_NAME = Component.translatable("block.minecraft.ominous_banner");
   private static final String RAIDERS_REMAINING = "event.minecraft.raid.raiders_remaining";
   public static final int VILLAGE_RADIUS_BUFFER = 16;
   private static final int POST_RAID_TICK_LIMIT = 40;
   private static final int DEFAULT_PRE_RAID_TICKS = 300;
   public static final int MAX_NO_ACTION_TIME = 2400;
   public static final int MAX_CELEBRATION_TICKS = 600;
   private static final int OUTSIDE_RAID_BOUNDS_TIMEOUT = 30;
   public static final int DEFAULT_MAX_RAID_OMEN_LEVEL = 5;
   private static final int LOW_MOB_THRESHOLD = 2;
   private static final Component RAID_NAME_COMPONENT = Component.translatable("event.minecraft.raid");
   private static final Component RAID_BAR_VICTORY_COMPONENT = Component.translatable("event.minecraft.raid.victory.full");
   private static final Component RAID_BAR_DEFEAT_COMPONENT = Component.translatable("event.minecraft.raid.defeat.full");
   private static final int HERO_OF_THE_VILLAGE_DURATION = 48000;
   private static final int VALID_RAID_RADIUS = 96;
   public static final int VALID_RAID_RADIUS_SQR = 9216;
   public static final int RAID_REMOVAL_THRESHOLD_SQR = 12544;
   private final Map<Integer, Raider> groupToLeaderMap = Maps.newHashMap();
   private final Map<Integer, Set<Raider>> groupRaiderMap = Maps.newHashMap();
   private final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
   private long ticksActive;
   private BlockPos center;
   private boolean started;
   private float totalHealth;
   private int raidOmenLevel;
   private boolean active;
   private int groupsSpawned;
   private int postRaidTicks;
   private int raidCooldownTicks;
   private final RandomSource random = RandomSource.create();
   private final ServerBossEvent raidEvent = new ServerBossEvent(
      Mth.createInsecureUUID(this.random), RAID_NAME_COMPONENT, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10
   );
   private final int numGroups;
   private Raid.RaidStatus status;
   private int celebrationTicks;
   private Optional<BlockPos> waveSpawnPos = Optional.empty();

   public Raid(final BlockPos center, final Difficulty difficulty) {
      this.active = true;
      this.raidCooldownTicks = 300;
      this.raidEvent.setProgress(0.0F);
      this.center = center;
      this.numGroups = this.getNumGroups(difficulty);
      this.status = Raid.RaidStatus.ONGOING;
   }

   private Raid(
      final boolean started,
      final boolean active,
      final long ticksActive,
      final int raidOmenLevel,
      final int groupsSpawned,
      final int raidCooldownTicks,
      final int postRaidTicks,
      final float totalHealth,
      final int numGroups,
      final Raid.RaidStatus status,
      final BlockPos center,
      final Set<UUID> heroesOfTheVillage
   ) {
      this.started = started;
      this.active = active;
      this.ticksActive = ticksActive;
      this.raidOmenLevel = raidOmenLevel;
      this.groupsSpawned = groupsSpawned;
      this.raidCooldownTicks = raidCooldownTicks;
      this.postRaidTicks = postRaidTicks;
      this.totalHealth = totalHealth;
      this.center = center;
      this.numGroups = numGroups;
      this.status = status;
      this.heroesOfTheVillage.addAll(heroesOfTheVillage);
   }

   public boolean isOver() {
      return this.isVictory() || this.isLoss();
   }

   public boolean isBetweenWaves() {
      return this.hasFirstWaveSpawned() && this.getTotalRaidersAlive() == 0 && this.raidCooldownTicks > 0;
   }

   public boolean hasFirstWaveSpawned() {
      return this.groupsSpawned > 0;
   }

   public boolean isStopped() {
      return this.status == Raid.RaidStatus.STOPPED;
   }

   public boolean isVictory() {
      return this.status == Raid.RaidStatus.VICTORY;
   }

   public boolean isLoss() {
      return this.status == Raid.RaidStatus.LOSS;
   }

   public float getTotalHealth() {
      return this.totalHealth;
   }

   public Set<Raider> getAllRaiders() {
      Set<Raider> raiders = Sets.newHashSet();

      for (Set<Raider> raiderSet : this.groupRaiderMap.values()) {
         raiders.addAll(raiderSet);
      }

      return raiders;
   }

   public boolean isStarted() {
      return this.started;
   }

   public int getGroupsSpawned() {
      return this.groupsSpawned;
   }

   private Predicate<ServerPlayer> validPlayer() {
      return player -> {
         BlockPos pos = player.blockPosition();
         return player.isAlive() && player.level().getRaidAt(pos) == this;
      };
   }

   private void updatePlayers(final ServerLevel level) {
      Set<ServerPlayer> currentPlayersInRaid = Sets.newHashSet(this.raidEvent.getPlayers());
      List<ServerPlayer> newPlayersInRaid = level.getPlayers(this.validPlayer());

      for (ServerPlayer player : newPlayersInRaid) {
         if (!currentPlayersInRaid.contains(player)) {
            this.raidEvent.addPlayer(player);
         }
      }

      for (ServerPlayer player : currentPlayersInRaid) {
         if (!newPlayersInRaid.contains(player)) {
            this.raidEvent.removePlayer(player);
         }
      }
   }

   public int getMaxRaidOmenLevel() {
      return 5;
   }

   public int getRaidOmenLevel() {
      return this.raidOmenLevel;
   }

   public void setRaidOmenLevel(final int raidOmenLevel) {
      this.raidOmenLevel = raidOmenLevel;
   }

   public boolean absorbRaidOmen(final ServerPlayer player) {
      MobEffectInstance effect = player.getEffect(MobEffects.RAID_OMEN);
      if (effect == null) {
         return false;
      }

      this.raidOmenLevel = this.raidOmenLevel + effect.getAmplifier() + 1;
      this.raidOmenLevel = Mth.clamp(this.raidOmenLevel, 0, this.getMaxRaidOmenLevel());
      if (!this.hasFirstWaveSpawned()) {
         player.awardStat(Stats.RAID_TRIGGER);
         CriteriaTriggers.RAID_OMEN.trigger(player);
      }

      return true;
   }

   public void stop() {
      this.active = false;
      this.raidEvent.removeAllPlayers();
      this.status = Raid.RaidStatus.STOPPED;
   }

   public void tick(final ServerLevel level) {
      if (!this.isStopped()) {
         if (this.status == Raid.RaidStatus.ONGOING) {
            boolean oldActive = this.active;
            this.active = level.hasChunkAt(this.center);
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
               this.stop();
               return;
            }

            if (oldActive != this.active) {
               this.raidEvent.setVisible(this.active);
            }

            if (!this.active) {
               return;
            }

            if (!level.isVillage(this.center)) {
               this.moveRaidCenterToNearbyVillageSection(level);
            }

            if (!level.isVillage(this.center)) {
               if (this.groupsSpawned > 0) {
                  this.status = Raid.RaidStatus.LOSS;
               } else {
                  this.stop();
               }
            }

            this.ticksActive++;
            if (this.ticksActive >= 48000L) {
               this.stop();
               return;
            }

            int raidersAlive = this.getTotalRaidersAlive();
            if (raidersAlive == 0 && this.hasMoreWaves()) {
               if (this.raidCooldownTicks <= 0) {
                  if (this.raidCooldownTicks == 0 && this.groupsSpawned > 0) {
                     this.raidCooldownTicks = 300;
                     this.raidEvent.setName(RAID_NAME_COMPONENT);
                     return;
                  }
               } else {
                  boolean hasCachedWaveSpawnPos = this.waveSpawnPos.isPresent();
                  boolean shouldTryToFindSpawnPos = !hasCachedWaveSpawnPos && this.raidCooldownTicks % 5 == 0;
                  if (hasCachedWaveSpawnPos && !level.isPositionEntityTicking(this.waveSpawnPos.get())) {
                     shouldTryToFindSpawnPos = true;
                  }

                  if (shouldTryToFindSpawnPos) {
                     this.waveSpawnPos = this.getValidSpawnPos(level);
                  }

                  if (this.raidCooldownTicks == 300 || this.raidCooldownTicks % 20 == 0) {
                     this.updatePlayers(level);
                  }

                  this.raidCooldownTicks--;
                  this.raidEvent.setProgress(Mth.clamp((300 - this.raidCooldownTicks) / 300.0F, 0.0F, 1.0F));
               }
            }

            if (this.ticksActive % 20L == 0L) {
               this.updatePlayers(level);
               this.updateRaiders(level);
               if (raidersAlive > 0) {
                  if (raidersAlive <= 2) {
                     this.raidEvent
                        .setName(
                           RAID_NAME_COMPONENT.copy().append(" - ").append(Component.translatable("event.minecraft.raid.raiders_remaining", raidersAlive))
                        );
                  } else {
                     this.raidEvent.setName(RAID_NAME_COMPONENT);
                  }
               } else {
                  this.raidEvent.setName(RAID_NAME_COMPONENT);
               }
            }

            if (SharedConstants.DEBUG_RAIDS) {
               this.raidEvent
                  .setName(
                     RAID_NAME_COMPONENT.copy()
                        .append(" wave: ")
                        .append(this.groupsSpawned + "")
                        .append(CommonComponents.SPACE)
                        .append("Raiders alive: ")
                        .append(this.getTotalRaidersAlive() + "")
                        .append(CommonComponents.SPACE)
                        .append(this.getHealthOfLivingRaiders() + "")
                        .append(" / ")
                        .append(this.totalHealth + "")
                        .append(" Is bonus? ")
                        .append((this.hasBonusWave() && this.hasSpawnedBonusWave()) + "")
                        .append(" Status: ")
                        .append(this.status.getSerializedName())
                  );
            }

            boolean soundPlayed = false;
            int attempt = 0;

            while (this.shouldSpawnGroup()) {
               BlockPos spawnPos = this.waveSpawnPos.orElseGet(() -> this.findRandomSpawnPos(level, 20));
               if (spawnPos != null) {
                  this.started = true;
                  this.spawnGroup(level, spawnPos);
                  if (!soundPlayed) {
                     this.playSound(level, spawnPos);
                     soundPlayed = true;
                  }
               } else {
                  attempt++;
               }

               if (attempt > 5) {
                  this.stop();
                  break;
               }
            }

            if (this.isStarted() && !this.hasMoreWaves() && raidersAlive == 0) {
               if (this.postRaidTicks < 40) {
                  this.postRaidTicks++;
               } else {
                  this.status = Raid.RaidStatus.VICTORY;

                  for (UUID heroUUID : this.heroesOfTheVillage) {
                     Entity entity = level.getEntity(heroUUID);
                     if (entity instanceof LivingEntity hero && !entity.isSpectator()) {
                        hero.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 48000, this.raidOmenLevel - 1, false, false, true));
                        if (hero instanceof ServerPlayer playerHero) {
                           playerHero.awardStat(Stats.RAID_WIN);
                           CriteriaTriggers.RAID_WIN.trigger(playerHero);
                        }
                     }
                  }
               }
            }

            this.setDirty(level);
         } else if (this.isOver()) {
            this.celebrationTicks++;
            if (this.celebrationTicks >= 600) {
               this.stop();
               return;
            }

            if (this.celebrationTicks % 20 == 0) {
               this.updatePlayers(level);
               this.raidEvent.setVisible(true);
               if (this.isVictory()) {
                  this.raidEvent.setProgress(0.0F);
                  this.raidEvent.setName(RAID_BAR_VICTORY_COMPONENT);
               } else {
                  this.raidEvent.setName(RAID_BAR_DEFEAT_COMPONENT);
               }
            }
         }
      }
   }

   private void moveRaidCenterToNearbyVillageSection(final ServerLevel level) {
      Stream<SectionPos> sectionsToSearchForVillage = SectionPos.cube(SectionPos.of(this.center), 2);
      sectionsToSearchForVillage.filter(level::isVillage)
         .map(SectionPos::center)
         .min(Comparator.comparingDouble(pos -> pos.distSqr(this.center)))
         .ifPresent(this::setCenter);
   }

   private Optional<BlockPos> getValidSpawnPos(final ServerLevel level) {
      BlockPos spawnPos = this.findRandomSpawnPos(level, 8);
      return spawnPos != null ? Optional.of(spawnPos) : Optional.empty();
   }

   private boolean hasMoreWaves() {
      return this.hasBonusWave() ? !this.hasSpawnedBonusWave() : !this.isFinalWave();
   }

   private boolean isFinalWave() {
      return this.getGroupsSpawned() == this.numGroups;
   }

   private boolean hasBonusWave() {
      return this.raidOmenLevel > 1;
   }

   private boolean hasSpawnedBonusWave() {
      return this.getGroupsSpawned() > this.numGroups;
   }

   private boolean shouldSpawnBonusGroup() {
      return this.isFinalWave() && this.getTotalRaidersAlive() == 0 && this.hasBonusWave();
   }

   private void updateRaiders(final ServerLevel level) {
      Iterator<Set<Raider>> raiders = this.groupRaiderMap.values().iterator();
      Set<Raider> toRemove = Sets.newHashSet();

      while (raiders.hasNext()) {
         Set<Raider> raiderSet = raiders.next();

         for (Raider raider : raiderSet) {
            BlockPos raiderPos = raider.blockPosition();
            if (raider.isRemoved() || raider.level().dimension() != level.dimension() || this.center.distSqr(raiderPos) >= 12544.0) {
               toRemove.add(raider);
            } else if (raider.tickCount > 600) {
               if (level.getEntity(raider.getUUID()) == null) {
                  toRemove.add(raider);
               }

               if (!level.isVillage(raiderPos) && raider.getNoActionTime() > 2400) {
                  raider.setTicksOutsideRaid(raider.getTicksOutsideRaid() + 1);
               }

               if (raider.getTicksOutsideRaid() >= 30) {
                  toRemove.add(raider);
               }
            }
         }
      }

      for (Raider raider : toRemove) {
         this.removeFromRaid(level, raider, true);
         if (raider.isPatrolLeader()) {
            this.removeLeader(raider.getWave());
         }
      }
   }

   private void playSound(final ServerLevel level, final BlockPos soundOrigin) {
      float distAway = 13.0F;
      int range = 64;
      Collection<ServerPlayer> playersInRaid = this.raidEvent.getPlayers();
      long seed = this.random.nextLong();

      for (ServerPlayer player : level.players()) {
         Vec3 playerLoc = player.position();
         Vec3 raidLoc = Vec3.atCenterOf(soundOrigin);
         double distBtwn = Math.sqrt((raidLoc.x - playerLoc.x) * (raidLoc.x - playerLoc.x) + (raidLoc.z - playerLoc.z) * (raidLoc.z - playerLoc.z));
         double x3 = playerLoc.x + 13.0 / distBtwn * (raidLoc.x - playerLoc.x);
         double z3 = playerLoc.z + 13.0 / distBtwn * (raidLoc.z - playerLoc.z);
         if (distBtwn <= 64.0 || playersInRaid.contains(player)) {
            player.connection.send(new ClientboundSoundPacket(SoundEvents.RAID_HORN, SoundSource.NEUTRAL, x3, player.getY(), z3, 64.0F, 1.0F, seed));
         }
      }
   }

   private void spawnGroup(final ServerLevel level, final BlockPos pos) {
      boolean leaderSet = false;
      int groupNumber = this.groupsSpawned + 1;
      this.totalHealth = 0.0F;
      DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
      boolean isBonusGroup = this.shouldSpawnBonusGroup();

      for (Raid.RaiderType raiderType : Raid.RaiderType.VALUES) {
         int numSpawns = this.getDefaultNumSpawns(raiderType, groupNumber, isBonusGroup)
            + this.getPotentialBonusSpawns(raiderType, this.random, groupNumber, difficulty, isBonusGroup);
         int ravagersSpawned = 0;

         for (int i = 0; i < numSpawns; i++) {
            Raider raider = raiderType.entityType.create(level, EntitySpawnReason.EVENT);
            if (raider == null) {
               break;
            }

            if (!leaderSet && raider.canBeLeader()) {
               raider.setPatrolLeader(true);
               this.setLeader(groupNumber, raider);
               leaderSet = true;
            }

            this.joinRaid(level, groupNumber, raider, pos, false);
            if (raiderType.entityType == EntityTypes.RAVAGER) {
               Raider ridingRaider = null;
               if (groupNumber == this.getNumGroups(Difficulty.NORMAL)) {
                  ridingRaider = EntityTypes.PILLAGER.create(level, EntitySpawnReason.EVENT);
               } else if (groupNumber >= this.getNumGroups(Difficulty.HARD)) {
                  if (ravagersSpawned == 0) {
                     ridingRaider = EntityTypes.EVOKER.create(level, EntitySpawnReason.EVENT);
                  } else {
                     ridingRaider = EntityTypes.VINDICATOR.create(level, EntitySpawnReason.EVENT);
                  }
               }

               ravagersSpawned++;
               if (ridingRaider != null) {
                  this.joinRaid(level, groupNumber, ridingRaider, pos, false);
                  ridingRaider.snapTo(pos, 0.0F, 0.0F);
                  ridingRaider.startRiding(raider, false, false);
               }
            }
         }
      }

      this.waveSpawnPos = Optional.empty();
      this.groupsSpawned++;
      this.updateBossbar();
      this.setDirty(level);
   }

   public void joinRaid(final ServerLevel level, final int groupNumber, final Raider raider, final @Nullable BlockPos pos, final boolean exists) {
      boolean added = this.addWaveMob(level, groupNumber, raider);
      if (added) {
         raider.setCurrentRaid(this);
         raider.setWave(groupNumber);
         raider.setCanJoinRaid(true);
         raider.setTicksOutsideRaid(0);
         if (!exists && pos != null) {
            raider.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            raider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
            raider.applyRaidBuffs(level, groupNumber, false);
            raider.setOnGround(true);
            level.addFreshEntityWithPassengers(raider);
         }
      }
   }

   public void updateBossbar() {
      this.raidEvent.setProgress(Mth.clamp(this.getHealthOfLivingRaiders() / this.totalHealth, 0.0F, 1.0F));
   }

   public float getHealthOfLivingRaiders() {
      float health = 0.0F;

      for (Set<Raider> raiders : this.groupRaiderMap.values()) {
         for (Raider raider : raiders) {
            health += raider.getHealth();
         }
      }

      return health;
   }

   private boolean shouldSpawnGroup() {
      return this.raidCooldownTicks == 0 && (this.groupsSpawned < this.numGroups || this.shouldSpawnBonusGroup()) && this.getTotalRaidersAlive() == 0;
   }

   public int getTotalRaidersAlive() {
      return this.groupRaiderMap.values().stream().mapToInt(Set::size).sum();
   }

   public void removeFromRaid(final ServerLevel level, final Raider raider, final boolean removeFromTotalHealth) {
      Set<Raider> raiders = this.groupRaiderMap.get(raider.getWave());
      if (raiders != null) {
         boolean couldRemove = raiders.remove(raider);
         if (couldRemove) {
            if (removeFromTotalHealth) {
               this.totalHealth = this.totalHealth - raider.getHealth();
            }

            raider.setCurrentRaid(null);
            this.updateBossbar();
            this.setDirty(level);
         }
      }
   }

   private void setDirty(final ServerLevel level) {
      level.getRaids().setDirty();
   }

   public static DataComponentPatch getBannerComponentPatch(final HolderGetter<BannerPattern> patternGetter) {
      DataComponentPatch.Builder builder = DataComponentPatch.builder();
      BannerPatternLayers patterns = new BannerPatternLayers.Builder()
         .addIfRegistered(patternGetter, BannerPatterns.RHOMBUS_MIDDLE, DyeColor.CYAN)
         .addIfRegistered(patternGetter, BannerPatterns.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY)
         .addIfRegistered(patternGetter, BannerPatterns.STRIPE_CENTER, DyeColor.GRAY)
         .addIfRegistered(patternGetter, BannerPatterns.BORDER, DyeColor.LIGHT_GRAY)
         .addIfRegistered(patternGetter, BannerPatterns.STRIPE_MIDDLE, DyeColor.BLACK)
         .addIfRegistered(patternGetter, BannerPatterns.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY)
         .addIfRegistered(patternGetter, BannerPatterns.CIRCLE_MIDDLE, DyeColor.LIGHT_GRAY)
         .addIfRegistered(patternGetter, BannerPatterns.BORDER, DyeColor.BLACK)
         .build();
      builder.set(DataComponents.BANNER_PATTERNS, patterns);
      builder.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.BANNER_PATTERNS, true));
      builder.set(DataComponents.ITEM_NAME, OMINOUS_BANNER_PATTERN_NAME);
      builder.set(DataComponents.RARITY, Rarity.UNCOMMON);
      return builder.build();
   }

   public static ItemStackTemplate getOminousBannerTemplate(final HolderGetter<BannerPattern> patternGetter) {
      return new ItemStackTemplate(Items.BANNER.white(), getBannerComponentPatch(patternGetter));
   }

   public static ItemStack getOminousBannerInstance(final HolderGetter<BannerPattern> patternGetter) {
      return getOminousBannerTemplate(patternGetter).create();
   }

   public @Nullable Raider getLeader(final int wave) {
      return this.groupToLeaderMap.get(wave);
   }

   private @Nullable BlockPos findRandomSpawnPos(final ServerLevel level, final int maxTries) {
      int secondsRemaining = this.raidCooldownTicks / 20;
      float howFar = 0.22F * secondsRemaining - 0.24F;
      BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
      float startAngle = this.random.nextFloat() * (float) (Math.PI * 2);

      for (int i = 0; i < maxTries; i++) {
         float angle = startAngle + (float) Math.PI * i / 8.0F;
         int spawnX = this.center.getX() + Mth.floor(Mth.cos(angle) * 32.0F * howFar) + this.random.nextInt(3) * Mth.floor(howFar);
         int spawnZ = this.center.getZ() + Mth.floor(Mth.sin(angle) * 32.0F * howFar) + this.random.nextInt(3) * Mth.floor(howFar);
         int spawnY = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawnX, spawnZ);
         if (Mth.abs(spawnY - this.center.getY()) <= 96) {
            spawnPos.set(spawnX, spawnY, spawnZ);
            if (!level.isVillage(spawnPos) || secondsRemaining <= 7) {
               int delta = 10;
               if (level.hasChunksAt(spawnPos.getX() - 10, spawnPos.getZ() - 10, spawnPos.getX() + 10, spawnPos.getZ() + 10)
                  && level.isPositionEntityTicking(spawnPos)
                  && (
                     RAVAGER_SPAWN_PLACEMENT_TYPE.isSpawnPositionOk(level, spawnPos, EntityTypes.RAVAGER)
                        || level.getBlockState(spawnPos.below()).is(Blocks.SNOW) && level.getBlockState(spawnPos).isAir()
                  )) {
                  return spawnPos;
               }
            }
         }
      }

      return null;
   }

   private boolean addWaveMob(final ServerLevel level, final int wave, final Raider raider) {
      return this.addWaveMob(level, wave, raider, true);
   }

   public boolean addWaveMob(final ServerLevel level, final int wave, final Raider raider, final boolean updateHealth) {
      this.groupRaiderMap.computeIfAbsent(wave, v -> Sets.newHashSet());
      Set<Raider> raiders = this.groupRaiderMap.get(wave);
      Raider existingCopy = null;

      for (Raider r : raiders) {
         if (r.getUUID().equals(raider.getUUID())) {
            existingCopy = r;
            break;
         }
      }

      if (existingCopy != null) {
         raiders.remove(existingCopy);
         raiders.add(raider);
      }

      raiders.add(raider);
      if (updateHealth) {
         this.totalHealth = this.totalHealth + raider.getHealth();
      }

      this.updateBossbar();
      this.setDirty(level);
      return true;
   }

   public void setLeader(final int wave, final Raider raider) {
      this.groupToLeaderMap.put(wave, raider);
      raider.setItemSlot(EquipmentSlot.HEAD, getOminousBannerInstance(raider.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
      raider.setDropChance(EquipmentSlot.HEAD, 2.0F);
   }

   public void removeLeader(final int wave) {
      this.groupToLeaderMap.remove(wave);
   }

   public BlockPos getCenter() {
      return this.center;
   }

   private void setCenter(final BlockPos center) {
      this.center = center;
   }

   private int getDefaultNumSpawns(final Raid.RaiderType type, final int wav, final boolean isBonusWave) {
      return isBonusWave ? type.spawnsPerWaveBeforeBonus[this.numGroups] : type.spawnsPerWaveBeforeBonus[wav];
   }

   private int getPotentialBonusSpawns(
      final Raid.RaiderType type, final RandomSource random, final int wav, final DifficultyInstance difficultyInstance, final boolean isBonusWave
   ) {
      Difficulty difficulty = difficultyInstance.getDifficulty();
      boolean isEasy = difficulty == Difficulty.EASY;
      boolean isNormal = difficulty == Difficulty.NORMAL;
      int bonusSpawns;
      switch (type) {
         case VINDICATOR:
         case PILLAGER:
            if (isEasy) {
               bonusSpawns = random.nextInt(2);
            } else if (isNormal) {
               bonusSpawns = 1;
            } else {
               bonusSpawns = 2;
            }
            break;
         case EVOKER:
         default:
            return 0;
         case WITCH:
            if (isEasy || wav <= 2 || wav == 4) {
               return 0;
            }

            bonusSpawns = 1;
            break;
         case RAVAGER:
            bonusSpawns = !isEasy && isBonusWave ? 1 : 0;
      }

      return bonusSpawns > 0 ? random.nextInt(bonusSpawns + 1) : 0;
   }

   public boolean isActive() {
      return this.active;
   }

   public int getNumGroups(final Difficulty difficulty) {
      return switch (difficulty) {
         case PEACEFUL -> 0;
         case EASY -> 3;
         case NORMAL -> 5;
         case HARD -> 7;
      };
   }

   public float getEnchantOdds() {
      int raidOmenLvl = this.getRaidOmenLevel();
      if (raidOmenLvl == 2) {
         return 0.1F;
      } else if (raidOmenLvl == 3) {
         return 0.25F;
      } else if (raidOmenLvl == 4) {
         return 0.5F;
      } else {
         return raidOmenLvl == 5 ? 0.75F : 0.0F;
      }
   }

   public void addHeroOfTheVillage(final Entity killer) {
      this.heroesOfTheVillage.add(killer.getUUID());
   }

   private enum RaidStatus implements StringRepresentable {
      ONGOING("ongoing"),
      VICTORY("victory"),
      LOSS("loss"),
      STOPPED("stopped");

      public static final Codec<Raid.RaidStatus> CODEC = StringRepresentable.fromEnum(Raid.RaidStatus::values);
      private final String name;

      RaidStatus(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   private enum RaiderType {
      VINDICATOR(EntityTypes.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
      EVOKER(EntityTypes.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
      PILLAGER(EntityTypes.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
      WITCH(EntityTypes.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
      RAVAGER(EntityTypes.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});

      private static final Raid.RaiderType[] VALUES = values();
      private final EntityType<? extends Raider> entityType;
      private final int[] spawnsPerWaveBeforeBonus;

      RaiderType(final EntityType<? extends Raider> entityType, final int[] spawnsPerWaveBeforeBonus) {
         this.entityType = entityType;
         this.spawnsPerWaveBeforeBonus = spawnsPerWaveBeforeBonus;
      }
   }
}
