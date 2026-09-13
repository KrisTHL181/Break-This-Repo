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
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

   public void apply(TrialSpawnerStateData.Packed p_409607_) {
      this.detectedPlayers.clear();
      this.detectedPlayers.addAll(p_409607_.detectedPlayers);
      this.currentMobs.clear();
      this.currentMobs.addAll(p_409607_.currentMobs);
      this.cooldownEndsAt = p_409607_.cooldownEndsAt;
      this.nextMobSpawnsAt = p_409607_.nextMobSpawnsAt;
      this.totalMobsSpawned = p_409607_.totalMobsSpawned;
      this.nextSpawnData = p_409607_.nextSpawnData;
      this.ejectingLootTable = p_409607_.ejectingLootTable;
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

   public boolean hasMobToSpawn(TrialSpawner p_407796_, RandomSource p_407785_) {
      boolean flag = this.getOrCreateNextSpawnData(p_407796_, p_407785_).getEntityToSpawn().getString("id").isPresent();
      return flag || !p_407796_.activeConfig().spawnPotentialsDefinition().isEmpty();
   }

   public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig p_406142_, int p_408318_) {
      return this.totalMobsSpawned >= p_406142_.calculateTargetTotalMobs(p_408318_);
   }

   public boolean haveAllCurrentMobsDied() {
      return this.currentMobs.isEmpty();
   }

   public boolean isReadyToSpawnNextMob(ServerLevel p_406783_, TrialSpawnerConfig p_408070_, int p_409326_) {
      return p_406783_.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < p_408070_.calculateTargetSimultaneousMobs(p_409326_);
   }

   public int countAdditionalPlayers(BlockPos p_406415_) {
      if (this.detectedPlayers.isEmpty()) {
         Util.logAndPauseIfInIde("Trial Spawner at " + p_406415_ + " has no detected players");
      }

      return Math.max(0, this.detectedPlayers.size() - 1);
   }

   public void tryDetectPlayers(ServerLevel p_409114_, BlockPos p_406572_, TrialSpawner p_408296_) {
      boolean flag = (p_406572_.asLong() + p_409114_.getGameTime()) % 20L != 0L;
      if (!flag && (!p_408296_.getState().equals(TrialSpawnerState.COOLDOWN) || !p_408296_.isOminous())) {
         List<UUID> list = p_408296_.getPlayerDetector().detect(p_409114_, p_408296_.getEntitySelector(), p_406572_, p_408296_.getRequiredPlayerRange(), true);
         boolean flag1;
         if (!p_408296_.isOminous() && !list.isEmpty()) {
            Optional<Pair<Player, Holder<MobEffect>>> optional = findPlayerWithOminousEffect(p_409114_, list);
            optional.ifPresent(p_449962_ -> {
               Player player = (Player)p_449962_.getFirst();
               if (p_449962_.getSecond() == MobEffects.BAD_OMEN) {
                  transformBadOmenIntoTrialOmen(player);
               }

               p_409114_.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
               p_408296_.applyOminous(p_409114_, p_406572_);
            });
            flag1 = optional.isPresent();
         } else {
            flag1 = false;
         }

         if (!p_408296_.getState().equals(TrialSpawnerState.COOLDOWN) || flag1) {
            boolean flag2 = p_408296_.getStateData().detectedPlayers.isEmpty();
            List<UUID> list1 = flag2
               ? list
               : p_408296_.getPlayerDetector().detect(p_409114_, p_408296_.getEntitySelector(), p_406572_, p_408296_.getRequiredPlayerRange(), false);
            if (this.detectedPlayers.addAll(list1)) {
               this.nextMobSpawnsAt = Math.max(p_409114_.getGameTime() + 40L, this.nextMobSpawnsAt);
               if (!flag1) {
                  int i = p_408296_.isOminous() ? 3019 : 3013;
                  p_409114_.levelEvent(i, p_406572_, this.detectedPlayers.size());
               }
            }
         }
      }
   }

   private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel p_406162_, List<UUID> p_409728_) {
      Player player = null;

      for (UUID uuid : p_409728_) {
         Player player1 = p_406162_.getPlayerByUUID(uuid);
         if (player1 != null) {
            Holder<MobEffect> holder = MobEffects.TRIAL_OMEN;
            if (player1.hasEffect(holder)) {
               return Optional.of(Pair.of(player1, holder));
            }

            if (player1.hasEffect(MobEffects.BAD_OMEN)) {
               player = player1;
            }
         }
      }

      return Optional.ofNullable(player).map(p_410005_ -> Pair.of(p_410005_, MobEffects.BAD_OMEN));
   }

   public void resetAfterBecomingOminous(TrialSpawner p_407388_, ServerLevel p_409364_) {
      this.currentMobs.stream().map(p_409364_::getEntity).forEach(p_410138_ -> {
         if (p_410138_ != null) {
            p_409364_.levelEvent(3012, p_410138_.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
            if (p_410138_ instanceof Mob mob) {
               mob.dropPreservedEquipment(p_409364_);
            }

            p_410138_.remove(Entity.RemovalReason.DISCARDED);
         }
      });
      if (!p_407388_.ominousConfig().spawnPotentialsDefinition().isEmpty()) {
         this.nextSpawnData = Optional.empty();
      }

      this.totalMobsSpawned = 0;
      this.currentMobs.clear();
      this.nextMobSpawnsAt = p_409364_.getGameTime() + p_407388_.ominousConfig().ticksBetweenSpawn();
      p_407388_.markUpdated();
      this.cooldownEndsAt = p_409364_.getGameTime() + p_407388_.ominousConfig().ticksBetweenItemSpawners();
   }

   private static void transformBadOmenIntoTrialOmen(Player p_408140_) {
      MobEffectInstance mobeffectinstance = p_408140_.getEffect(MobEffects.BAD_OMEN);
      if (mobeffectinstance != null) {
         int i = mobeffectinstance.getAmplifier() + 1;
         int j = 18000 * i;
         p_408140_.removeEffect(MobEffects.BAD_OMEN);
         p_408140_.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, j, 0));
      }
   }

   public boolean isReadyToOpenShutter(ServerLevel p_405983_, float p_406218_, int p_409489_) {
      long i = this.cooldownEndsAt - p_409489_;
      return (float)p_405983_.getGameTime() >= (float)i + p_406218_;
   }

   public boolean isReadyToEjectItems(ServerLevel p_410027_, float p_409204_, int p_409749_) {
      long i = this.cooldownEndsAt - p_409749_;
      return (float)(p_410027_.getGameTime() - i) % p_409204_ == 0.0F;
   }

   public boolean isCooldownFinished(ServerLevel p_410327_) {
      return p_410327_.getGameTime() >= this.cooldownEndsAt;
   }

   protected SpawnData getOrCreateNextSpawnData(TrialSpawner p_409559_, RandomSource p_409480_) {
      if (this.nextSpawnData.isPresent()) {
         return this.nextSpawnData.get();
      }

      WeightedList<SpawnData> weightedlist = p_409559_.activeConfig().spawnPotentialsDefinition();
      Optional<SpawnData> optional = weightedlist.isEmpty() ? this.nextSpawnData : weightedlist.getRandom(p_409480_);
      this.nextSpawnData = Optional.of(optional.orElseGet(SpawnData::new));
      p_409559_.markUpdated();
      return this.nextSpawnData.get();
   }

   public @Nullable Entity getOrCreateDisplayEntity(TrialSpawner p_407095_, Level p_410216_, TrialSpawnerState p_406028_) {
      if (!p_406028_.hasSpinningMob()) {
         return null;
      }

      if (this.displayEntity == null) {
         CompoundTag compoundtag = this.getOrCreateNextSpawnData(p_407095_, p_410216_.getRandom()).getEntityToSpawn();
         if (compoundtag.getString("id").isPresent()) {
            this.displayEntity = EntityType.loadEntityRecursive(compoundtag, p_410216_, EntitySpawnReason.TRIAL_SPAWNER, EntityProcessor.NOP);
         }
      }

      return this.displayEntity;
   }

   public CompoundTag getUpdateTag(TrialSpawnerState p_410288_) {
      CompoundTag compoundtag = new CompoundTag();
      if (p_410288_ == TrialSpawnerState.ACTIVE) {
         compoundtag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
      }

      this.nextSpawnData.ifPresent(p_406057_ -> compoundtag.store("spawn_data", SpawnData.CODEC, p_406057_));
      return compoundtag;
   }

   public double getSpin() {
      return this.spin;
   }

   public double getOSpin() {
      return this.oSpin;
   }

   WeightedList<ItemStack> getDispensingItems(ServerLevel p_407781_, TrialSpawnerConfig p_409970_, BlockPos p_408739_) {
      if (this.dispensing != null) {
         return this.dispensing;
      }

      LootTable loottable = p_407781_.getServer().reloadableRegistries().getLootTable(p_409970_.itemsToDropWhenOminous());
      LootParams lootparams = new LootParams.Builder(p_407781_).create(LootContextParamSets.EMPTY);
      long i = lowResolutionPosition(p_407781_, p_408739_);
      ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, i);
      if (objectarraylist.isEmpty()) {
         return WeightedList.of();
      }

      WeightedList.Builder<ItemStack> builder = WeightedList.builder();
      ObjectListIterator var10 = objectarraylist.iterator();

      while (var10.hasNext()) {
         ItemStack itemstack = (ItemStack)var10.next();
         builder.add(itemstack.copyWithCount(1), itemstack.getCount());
      }

      this.dispensing = builder.build();
      return this.dispensing;
   }

   private static long lowResolutionPosition(ServerLevel p_409350_, BlockPos p_409258_) {
      BlockPos blockpos = new BlockPos(Mth.floor(p_409258_.getX() / 30.0F), Mth.floor(p_409258_.getY() / 20.0F), Mth.floor(p_409258_.getZ() / 30.0F));
      return p_409350_.getSeed() + blockpos.asLong();
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
         p_408398_ -> p_408398_.group(
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Set.of()).forGetter(TrialSpawnerStateData.Packed::detectedPlayers),
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Set.of()).forGetter(TrialSpawnerStateData.Packed::currentMobs),
               Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", 0L).forGetter(TrialSpawnerStateData.Packed::cooldownEndsAt),
               Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", 0L).forGetter(TrialSpawnerStateData.Packed::nextMobSpawnsAt),
               Codec.intRange(0, Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", 0).forGetter(TrialSpawnerStateData.Packed::totalMobsSpawned),
               SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(TrialSpawnerStateData.Packed::nextSpawnData),
               LootTable.KEY_CODEC.lenientOptionalFieldOf("ejecting_loot_table").forGetter(TrialSpawnerStateData.Packed::ejectingLootTable)
            )
            .apply(p_408398_, TrialSpawnerStateData.Packed::new)
      );
   }
}
