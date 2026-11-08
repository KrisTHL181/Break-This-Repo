package net.minecraft.world.level;

import net.minecraft.server.level.ServerLevel;

public interface CustomSpawner {
   void tick(ServerLevel level, boolean spawnEnemies);
}
