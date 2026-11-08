package net.minecraft.world.level.storage;

public interface WritableLevelData extends LevelData {
   void setSpawn(final LevelData.RespawnData respawnData);
}
