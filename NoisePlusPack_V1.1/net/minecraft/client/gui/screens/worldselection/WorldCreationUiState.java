package net.minecraft.client.gui.screens.worldselection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.util.FileUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WorldCreationUiState {
   private static final Component DEFAULT_WORLD_NAME = Component.translatable("selectWorld.newWorld");
   private final List<Consumer<WorldCreationUiState>> listeners = new ArrayList<>();
   private String name = DEFAULT_WORLD_NAME.getString();
   private WorldCreationUiState.SelectedGameMode gameMode = WorldCreationUiState.SelectedGameMode.SURVIVAL;
   private Difficulty difficulty = Difficulty.NORMAL;
   private @Nullable Boolean allowCommands;
   private String seed;
   private boolean generateStructures;
   private boolean bonusChest;
   private final Path savesFolder;
   private String targetFolder;
   private WorldCreationContext settings;
   private WorldCreationUiState.WorldTypeEntry worldType;
   private final List<WorldCreationUiState.WorldTypeEntry> normalPresetList = new ArrayList<>();
   private final List<WorldCreationUiState.WorldTypeEntry> altPresetList = new ArrayList<>();
   private GameRules gameRules;

   public WorldCreationUiState(Path p_276024_, WorldCreationContext p_276050_, Optional<ResourceKey<WorldPreset>> p_276022_, OptionalLong p_276014_) {
      this.savesFolder = p_276024_;
      this.settings = p_276050_;
      this.worldType = new WorldCreationUiState.WorldTypeEntry(findPreset(p_276050_, p_276022_).orElse(null));
      this.updatePresetLists();
      this.seed = p_276014_.isPresent() ? Long.toString(p_276014_.getAsLong()) : "";
      this.generateStructures = p_276050_.options().generateStructures();
      this.bonusChest = p_276050_.options().generateBonusChest();
      this.targetFolder = this.findResultFolder(this.name);
      this.gameMode = p_276050_.initialWorldCreationOptions().selectedGameMode();
      this.gameRules = new GameRules(p_276050_.dataConfiguration().enabledFeatures());
      this.gameRules.setAll(p_276050_.initialWorldCreationOptions().gameRuleOverwrites(), null);
      Optional.ofNullable(p_276050_.initialWorldCreationOptions().flatLevelPreset())
         .flatMap(p_357752_ -> p_276050_.worldgenLoadContext().lookup(Registries.FLAT_LEVEL_GENERATOR_PRESET).flatMap(p_357754_ -> p_357754_.get(p_357752_)))
         .map(p_357749_ -> p_357749_.value().settings())
         .ifPresent(p_357750_ -> this.updateDimensions(PresetEditor.flatWorldConfigurator(p_357750_)));
   }

   public void addListener(Consumer<WorldCreationUiState> p_267938_) {
      this.listeners.add(p_267938_);
   }

   public void onChanged() {
      boolean flag = this.isBonusChest();
      if (flag != this.settings.options().generateBonusChest()) {
         this.settings = this.settings.withOptions(p_268360_ -> p_268360_.withBonusChest(flag));
      }

      boolean flag1 = this.isGenerateStructures();
      if (flag1 != this.settings.options().generateStructures()) {
         this.settings = this.settings.withOptions(p_267945_ -> p_267945_.withStructures(flag1));
      }

      for (Consumer<WorldCreationUiState> consumer : this.listeners) {
         consumer.accept(this);
      }
   }

   public void setName(String p_268167_) {
      this.name = p_268167_;
      this.targetFolder = this.findResultFolder(p_268167_);
      this.onChanged();
   }

   private String findResultFolder(String p_276032_) {
      String s = p_276032_.trim();

      try {
         return FileUtil.findAvailableName(this.savesFolder, !s.isEmpty() ? s : DEFAULT_WORLD_NAME.getString(), "");
      } catch (Exception exception) {
         try {
            return FileUtil.findAvailableName(this.savesFolder, "World", "");
         } catch (IOException ioexception) {
            throw new RuntimeException("Could not create save folder", ioexception);
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public String getTargetFolder() {
      return this.targetFolder;
   }

   public void setGameMode(WorldCreationUiState.SelectedGameMode p_268231_) {
      this.gameMode = p_268231_;
      this.onChanged();
   }

   public WorldCreationUiState.SelectedGameMode getGameMode() {
      return this.isDebug() ? WorldCreationUiState.SelectedGameMode.DEBUG : this.gameMode;
   }

   public void setDifficulty(Difficulty p_268032_) {
      this.difficulty = p_268032_;
      this.onChanged();
   }

   public Difficulty getDifficulty() {
      return this.isHardcore() ? Difficulty.HARD : this.difficulty;
   }

   public boolean isHardcore() {
      return this.getGameMode() == WorldCreationUiState.SelectedGameMode.HARDCORE;
   }

   public void setAllowCommands(boolean p_327747_) {
      this.allowCommands = p_327747_;
      this.onChanged();
   }

   public boolean isAllowCommands() {
      if (this.isDebug()) {
         return true;
      } else if (this.isHardcore()) {
         return false;
      } else {
         return this.allowCommands == null ? this.getGameMode() == WorldCreationUiState.SelectedGameMode.CREATIVE : this.allowCommands;
      }
   }

   public void setSeed(String p_268100_) {
      this.seed = p_268100_;
      this.settings = this.settings.withOptions(p_267957_ -> p_267957_.withSeed(WorldOptions.parseSeed(this.getSeed())));
      this.onChanged();
   }

   public String getSeed() {
      return this.seed;
   }

   public void setGenerateStructures(boolean p_268090_) {
      this.generateStructures = p_268090_;
      this.onChanged();
   }

   public boolean isGenerateStructures() {
      return this.isDebug() ? false : this.generateStructures;
   }

   public void setBonusChest(boolean p_268236_) {
      this.bonusChest = p_268236_;
      this.onChanged();
   }

   public boolean isBonusChest() {
      return !this.isDebug() && !this.isHardcore() ? this.bonusChest : false;
   }

   public void setSettings(WorldCreationContext p_268313_) {
      this.settings = p_268313_;
      this.updatePresetLists();
      this.onChanged();
   }

   public WorldCreationContext getSettings() {
      return this.settings;
   }

   public void updateDimensions(WorldCreationContext.DimensionsUpdater p_268314_) {
      this.settings = this.settings.withDimensions(p_268314_);
      this.onChanged();
   }

   protected boolean tryUpdateDataConfiguration(WorldDataConfiguration p_268016_) {
      WorldDataConfiguration worlddataconfiguration = this.settings.dataConfiguration();
      if (worlddataconfiguration.dataPacks().getEnabled().equals(p_268016_.dataPacks().getEnabled())
         && worlddataconfiguration.enabledFeatures().equals(p_268016_.enabledFeatures())) {
         this.settings = new WorldCreationContext(
            this.settings.options(),
            this.settings.datapackDimensions(),
            this.settings.selectedDimensions(),
            this.settings.worldgenRegistries(),
            this.settings.dataPackResources(),
            p_268016_,
            this.settings.initialWorldCreationOptions()
         );
         return true;
      } else {
         return false;
      }
   }

   public boolean isDebug() {
      return this.settings.selectedDimensions().isDebug();
   }

   public void setWorldType(WorldCreationUiState.WorldTypeEntry p_268117_) {
      this.worldType = p_268117_;
      Holder<WorldPreset> holder = p_268117_.preset();
      if (holder != null) {
         this.updateDimensions((p_268134_, p_268035_) -> holder.value().createWorldDimensions());
      }
   }

   public WorldCreationUiState.WorldTypeEntry getWorldType() {
      return this.worldType;
   }

   public @Nullable PresetEditor getPresetEditor() {
      Holder<WorldPreset> holder = this.getWorldType().preset();
      return holder != null ? PresetEditor.EDITORS.get(holder.unwrapKey()) : null;
   }

   public List<WorldCreationUiState.WorldTypeEntry> getNormalPresetList() {
      return this.normalPresetList;
   }

   public List<WorldCreationUiState.WorldTypeEntry> getAltPresetList() {
      return this.altPresetList;
   }

   private void updatePresetLists() {
      Registry<WorldPreset> registry = this.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET);
      this.normalPresetList.clear();
      this.normalPresetList
         .addAll(
            getNonEmptyList(registry, WorldPresetTags.NORMAL).orElseGet(() -> registry.listElements().map(WorldCreationUiState.WorldTypeEntry::new).toList())
         );
      this.altPresetList.clear();
      this.altPresetList.addAll(getNonEmptyList(registry, WorldPresetTags.EXTENDED).orElse(this.normalPresetList));
      Holder<WorldPreset> holder = this.worldType.preset();
      if (holder != null) {
         WorldCreationUiState.WorldTypeEntry worldcreationuistate$worldtypeentry = findPreset(this.getSettings(), holder.unwrapKey())
            .map(WorldCreationUiState.WorldTypeEntry::new)
            .orElse(this.normalPresetList.getFirst());
         boolean flag = PresetEditor.EDITORS.get(holder.unwrapKey()) != null;
         if (flag) {
            this.worldType = worldcreationuistate$worldtypeentry;
         } else {
            this.setWorldType(worldcreationuistate$worldtypeentry);
         }
      }
   }

   private static Optional<Holder<WorldPreset>> findPreset(WorldCreationContext p_268025_, Optional<ResourceKey<WorldPreset>> p_268184_) {
      return p_268184_.flatMap(p_389323_ -> p_268025_.worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).get((ResourceKey<WorldPreset>)p_389323_));
   }

   private static Optional<List<WorldCreationUiState.WorldTypeEntry>> getNonEmptyList(Registry<WorldPreset> p_268296_, TagKey<WorldPreset> p_268097_) {
      return p_268296_.get(p_268097_)
         .map(p_268149_ -> p_268149_.stream().map(WorldCreationUiState.WorldTypeEntry::new).toList())
         .filter(p_268066_ -> !p_268066_.isEmpty());
   }

   public void setGameRules(GameRules p_458738_) {
      this.gameRules = p_458738_;
      this.onChanged();
   }

   public GameRules getGameRules() {
      return this.gameRules;
   }

   @OnlyIn(Dist.CLIENT)
   public enum SelectedGameMode {
      SURVIVAL("survival", GameType.SURVIVAL),
      HARDCORE("hardcore", GameType.SURVIVAL),
      CREATIVE("creative", GameType.CREATIVE),
      DEBUG("spectator", GameType.SPECTATOR);

      public final GameType gameType;
      public final Component displayName;
      private final Component info;

      SelectedGameMode(final String p_268033_, final GameType p_268252_) {
         this.gameType = p_268252_;
         this.displayName = Component.translatable("selectWorld.gameMode." + p_268033_);
         this.info = Component.translatable("selectWorld.gameMode." + p_268033_ + ".info");
      }

      public Component getInfo() {
         return this.info;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record WorldTypeEntry(@Nullable Holder<WorldPreset> preset) {
      private static final Component CUSTOM_WORLD_DESCRIPTION = Component.translatable("generator.custom");

      public Component describePreset() {
         return Optional.ofNullable(this.preset)
            .flatMap(Holder::unwrapKey)
            .map(p_448106_ -> Component.translatable(p_448106_.identifier().toLanguageKey("generator")))
            .orElse(CUSTOM_WORLD_DESCRIPTION);
      }

      public boolean isAmplified() {
         return Optional.ofNullable(this.preset).flatMap(Holder::unwrapKey).filter(p_268224_ -> p_268224_.equals(WorldPresets.AMPLIFIED)).isPresent();
      }
   }
}
