package net.minecraft.server;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.storage.LevelDataAndDimensions;

public record WorldStem(
   CloseableResourceManager resourceManager,
   ReloadableServerResources dataPackResources,
   LayeredRegistryAccess<RegistryLayer> registries,
   LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings
) implements AutoCloseable {
   @Override
   public void close() {
      this.resourceManager.close();
   }
}
