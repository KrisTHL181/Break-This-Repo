package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.Difficulty;

public record LevelSettings(
   String levelName, GameType gameType, LevelSettings.DifficultySettings difficultySettings, boolean allowCommands, WorldDataConfiguration dataConfiguration
) {
   public static LevelSettings parse(final Dynamic<?> input, final WorldDataConfiguration loadConfig) {
      GameType gameType = GameType.byId(input.get("GameType").asInt(0));
      return new LevelSettings(
         input.get("LevelName").asString(""),
         gameType,
         input.get("difficulty_settings").read(LevelSettings.DifficultySettings.CODEC).result().orElse(LevelSettings.DifficultySettings.DEFAULT),
         input.get("allowCommands").asBoolean(gameType == GameType.CREATIVE),
         loadConfig
      );
   }

   public LevelSettings withGameType(final GameType gameType) {
      return new LevelSettings(this.levelName, gameType, this.difficultySettings, this.allowCommands, this.dataConfiguration);
   }

   public LevelSettings withAllowCommands(final boolean allowCommands) {
      return new LevelSettings(this.levelName, this.gameType, this.difficultySettings, allowCommands, this.dataConfiguration);
   }

   public LevelSettings withDifficulty(final Difficulty difficulty) {
      return new LevelSettings(
         this.levelName,
         this.gameType,
         new LevelSettings.DifficultySettings(difficulty, this.difficultySettings.hardcore(), this.difficultySettings.locked()),
         this.allowCommands,
         this.dataConfiguration
      );
   }

   public LevelSettings withDifficultyLock(final boolean locked) {
      return new LevelSettings(
         this.levelName,
         this.gameType,
         new LevelSettings.DifficultySettings(this.difficultySettings.difficulty(), this.difficultySettings.hardcore(), locked),
         this.allowCommands,
         this.dataConfiguration
      );
   }

   public LevelSettings withDataConfiguration(final WorldDataConfiguration dataConfiguration) {
      return new LevelSettings(this.levelName, this.gameType, this.difficultySettings, this.allowCommands, dataConfiguration);
   }

   public LevelSettings copy() {
      return new LevelSettings(this.levelName, this.gameType, this.difficultySettings, this.allowCommands, this.dataConfiguration);
   }

   public record DifficultySettings(Difficulty difficulty, boolean hardcore, boolean locked) {
      public static final LevelSettings.DifficultySettings DEFAULT = new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false);
      public static final Codec<LevelSettings.DifficultySettings> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Difficulty.CODEC.fieldOf("difficulty").forGetter(LevelSettings.DifficultySettings::difficulty),
               Codec.BOOL.fieldOf("hardcore").forGetter(LevelSettings.DifficultySettings::hardcore),
               Codec.BOOL.fieldOf("locked").forGetter(LevelSettings.DifficultySettings::locked)
            )
            .apply(i, LevelSettings.DifficultySettings::new)
      );
   }
}
