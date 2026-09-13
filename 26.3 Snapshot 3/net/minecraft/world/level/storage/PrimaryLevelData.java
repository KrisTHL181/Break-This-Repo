package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PrimaryLevelData implements ServerLevelData, WorldData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String LEVEL_NAME = "LevelName";
   protected static final String OLD_PLAYER = "Player";
   protected static final String SINGLEPLAYER_UUID = "singleplayer_uuid";
   protected static final String OLD_WORLD_GEN_SETTINGS = "WorldGenSettings";
   private LevelSettings settings;
   private final PrimaryLevelData.SpecialWorldProperty specialWorldProperty;
   private final Lifecycle worldGenSettingsLifecycle;
   private LevelData.RespawnData respawnData;
   private long gameTime;
   private final @Nullable UUID singlePlayerUUID;
   private final int version;
   private boolean initialized;
   private final Set<String> knownServerBrands;
   private boolean wasModded;
   private final Set<String> removedFeatureFlags;

   private PrimaryLevelData(
      final @Nullable UUID singlePlayerUUID,
      final boolean wasModded,
      final LevelData.RespawnData respawnData,
      final long gameTime,
      final int version,
      final boolean initialized,
      final Set<String> knownServerBrands,
      final Set<String> removedFeatureFlags,
      final LevelSettings settings,
      final PrimaryLevelData.SpecialWorldProperty specialWorldProperty,
      final Lifecycle worldGenSettingsLifecycle
   ) {
      this.wasModded = wasModded;
      this.respawnData = respawnData;
      this.gameTime = gameTime;
      this.version = version;
      this.initialized = initialized;
      this.knownServerBrands = knownServerBrands;
      this.removedFeatureFlags = removedFeatureFlags;
      this.singlePlayerUUID = singlePlayerUUID;
      this.settings = settings;
      this.specialWorldProperty = specialWorldProperty;
      this.worldGenSettingsLifecycle = worldGenSettingsLifecycle;
   }

   public PrimaryLevelData(final LevelSettings levelSettings, final PrimaryLevelData.SpecialWorldProperty specialWorldProperty, final Lifecycle lifecycle) {
      this(
         null,
         false,
         LevelData.RespawnData.DEFAULT,
         0L,
         19133,
         false,
         Sets.newLinkedHashSet(),
         new HashSet<>(),
         levelSettings.copy(),
         specialWorldProperty,
         lifecycle
      );
   }

   public static <T> PrimaryLevelData parse(
      final Dynamic<T> input,
      final LevelSettings settings,
      final PrimaryLevelData.SpecialWorldProperty specialWorldProperty,
      final Lifecycle worldGenSettingsLifecycle
   ) {
      long gameTime = input.get("Time").asLong(0L);
      LevelVersion levelVersion = LevelVersion.parse(input);
      return new PrimaryLevelData(
         (UUID)input.get("singleplayer_uuid").flatMap(UUIDUtil.CODEC::parse).result().orElse(null),
         input.get("WasModded").asBoolean(false),
         input.get("spawn").read(LevelData.RespawnData.CODEC).result().orElse(LevelData.RespawnData.DEFAULT),
         gameTime,
         levelVersion.levelDataVersion(),
         input.get("initialized").asBoolean(true),
         input.get("ServerBrands").asStream().flatMap(b -> b.asString().result().stream()).collect(Collectors.toCollection(Sets::newLinkedHashSet)),
         input.get("removed_features").asStream().flatMap(b -> b.asString().result().stream()).collect(Collectors.toSet()),
         settings,
         specialWorldProperty,
         worldGenSettingsLifecycle
      );
   }

   @Override
   public CompoundTag createTag(@Nullable UUID singlePlayerUUID) {
      if (singlePlayerUUID == null) {
         singlePlayerUUID = this.singlePlayerUUID;
      }

      CompoundTag tag = new CompoundTag();
      this.setTagData(tag, singlePlayerUUID);
      return tag;
   }

   private void setTagData(final CompoundTag tag, final @Nullable UUID singlePlayerUUID) {
      tag.put("ServerBrands", stringCollectionToTag(this.knownServerBrands));
      tag.putBoolean("WasModded", this.wasModded);
      if (!this.removedFeatureFlags.isEmpty()) {
         tag.put("removed_features", stringCollectionToTag(this.removedFeatureFlags));
      }

      writeVersionTag(tag);
      NbtUtils.addCurrentDataVersion(tag);
      tag.putInt("GameType", this.settings.gameType().getId());
      tag.store("spawn", LevelData.RespawnData.CODEC, this.respawnData);
      tag.putLong("Time", this.gameTime);
      writeLastPlayed(tag);
      tag.putString("LevelName", this.settings.levelName());
      tag.putInt("version", 19133);
      tag.putBoolean("allowCommands", this.settings.allowCommands());
      tag.putBoolean("initialized", this.initialized);
      tag.store("difficulty_settings", LevelSettings.DifficultySettings.CODEC, this.settings.difficultySettings());
      if (singlePlayerUUID != null) {
         tag.storeNullable("singleplayer_uuid", UUIDUtil.CODEC, singlePlayerUUID);
      }

      tag.store(WorldDataConfiguration.MAP_CODEC, this.settings.dataConfiguration());
   }

   public static void writeLastPlayed(final CompoundTag tag) {
      tag.putLong("LastPlayed", Util.getEpochMillis());
   }

   public static Dynamic<?> writeLastPlayed(final Dynamic<?> tag) {
      return tag.set("LastPlayed", tag.createLong(Util.getEpochMillis()));
   }

   public static void writeVersionTag(final CompoundTag tag) {
      CompoundTag worldVersion = new CompoundTag();
      worldVersion.putString("Name", SharedConstants.getCurrentVersion().name());
      worldVersion.putInt("Id", SharedConstants.getCurrentVersion().dataVersion().version());
      worldVersion.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().stable());
      worldVersion.putString("Series", SharedConstants.getCurrentVersion().dataVersion().series());
      tag.put("Version", worldVersion);
   }

   public static Dynamic<?> writeVersionTag(final Dynamic<?> tag) {
      Dynamic<?> worldVersion = tag.emptyMap()
         .set("Name", tag.createString(SharedConstants.getCurrentVersion().name()))
         .set("Id", tag.createInt(SharedConstants.getCurrentVersion().dataVersion().version()))
         .set("Snapshot", tag.createBoolean(!SharedConstants.getCurrentVersion().stable()))
         .set("Series", tag.createString(SharedConstants.getCurrentVersion().dataVersion().series()));
      return tag.set("Version", worldVersion);
   }

   private static ListTag stringCollectionToTag(final Set<String> values) {
      ListTag result = new ListTag();
      values.stream().map(StringTag::valueOf).forEach(result::add);
      return result;
   }

   @Override
   public LevelData.RespawnData getRespawnData() {
      return this.respawnData;
   }

   @Override
   public long getGameTime() {
      return this.gameTime;
   }

   @Override
   public @Nullable UUID getSinglePlayerUUID() {
      return this.singlePlayerUUID;
   }

   @Override
   public void setGameTime(final long time) {
      this.gameTime = time;
   }

   @Override
   public void setSpawn(final LevelData.RespawnData respawnData) {
      this.respawnData = respawnData;
   }

   @Override
   public String getLevelName() {
      return this.settings.levelName();
   }

   @Override
   public int getVersion() {
      return this.version;
   }

   @Override
   public GameType getGameType() {
      return this.settings.gameType();
   }

   @Override
   public void setGameType(final GameType gameType) {
      this.settings = this.settings.withGameType(gameType);
   }

   @Override
   public boolean isHardcore() {
      return this.settings.difficultySettings().hardcore();
   }

   @Override
   public boolean isAllowCommands() {
      return this.settings.allowCommands();
   }

   @Override
   public void setAllowCommands(final boolean allowCommands) {
      this.settings = this.settings.withAllowCommands(allowCommands);
   }

   @Override
   public boolean isInitialized() {
      return this.initialized;
   }

   @Override
   public void setInitialized(final boolean initialized) {
      this.initialized = initialized;
   }

   @Override
   public Difficulty getDifficulty() {
      return this.settings.difficultySettings().difficulty();
   }

   @Override
   public void setDifficulty(final Difficulty difficulty) {
      this.settings = this.settings.withDifficulty(difficulty);
   }

   @Override
   public boolean isDifficultyLocked() {
      return this.settings.difficultySettings().locked();
   }

   @Override
   public void setDifficultyLocked(final boolean difficultyLocked) {
      this.settings = this.settings.withDifficultyLock(difficultyLocked);
   }

   @Override
   public void fillCrashReportCategory(final CrashReportCategory category, final LevelHeightAccessor levelHeightAccessor) {
      ServerLevelData.super.fillCrashReportCategory(category, levelHeightAccessor);
      WorldData.super.fillCrashReportCategory(category);
   }

   @Override
   public boolean isFlatWorld() {
      return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.FLAT;
   }

   @Override
   public boolean isDebugWorld() {
      return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.DEBUG;
   }

   @Override
   public Lifecycle worldGenSettingsLifecycle() {
      return this.worldGenSettingsLifecycle;
   }

   @Override
   public WorldDataConfiguration getDataConfiguration() {
      return this.settings.dataConfiguration();
   }

   @Override
   public void setDataConfiguration(final WorldDataConfiguration dataConfiguration) {
      this.settings = this.settings.withDataConfiguration(dataConfiguration);
   }

   @Override
   public void setModdedInfo(final String serverBrand, final boolean isModded) {
      this.knownServerBrands.add(serverBrand);
      this.wasModded |= isModded;
   }

   @Override
   public boolean wasModded() {
      return this.wasModded;
   }

   @Override
   public Set<String> getKnownServerBrands() {
      return ImmutableSet.copyOf(this.knownServerBrands);
   }

   @Override
   public Set<String> getRemovedFeatureFlags() {
      return Set.copyOf(this.removedFeatureFlags);
   }

   @Override
   public ServerLevelData overworldData() {
      return this;
   }

   @Override
   public LevelSettings getLevelSettings() {
      return this.settings.copy();
   }

   @Deprecated
   public enum SpecialWorldProperty {
      NONE,
      FLAT,
      DEBUG;
   }
}
