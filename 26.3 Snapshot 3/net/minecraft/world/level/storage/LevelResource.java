package net.minecraft.world.level.storage;

public record LevelResource(String id) {
   public static final LevelResource PLAYER_ADVANCEMENTS_DIR = new LevelResource("players/advancements");
   public static final LevelResource PLAYER_STATS_DIR = new LevelResource("players/stats");
   public static final LevelResource PLAYER_DATA_DIR = new LevelResource("players/data");
   public static final LevelResource PLAYER_OLD_DATA_DIR = new LevelResource("players");
   public static final LevelResource LEVEL_DATA_FILE = new LevelResource("level.dat");
   public static final LevelResource OLD_LEVEL_DATA_FILE = new LevelResource("level.dat_old");
   public static final LevelResource ICON_FILE = new LevelResource("icon.png");
   public static final LevelResource LOCK_FILE = new LevelResource("session.lock");
   public static final LevelResource GENERATED_DIR = new LevelResource("generated");
   public static final LevelResource DATAPACK_DIR = new LevelResource("datapacks");
   public static final LevelResource MAP_RESOURCE_FILE = new LevelResource("resourcepacks/resources.zip");
   public static final LevelResource DATA = new LevelResource("data");
   public static final LevelResource ROOT = new LevelResource(".");

   @Override
   public String toString() {
      return "/" + this.id;
   }
}
