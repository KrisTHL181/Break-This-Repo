package net.minecraft.client.gui.screens.worldselection;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface CreateWorldCallback {
   boolean create(
      CreateWorldScreen createWorldScreen,
      LayeredRegistryAccess<RegistryLayer> finalLayers,
      LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings,
      Optional<GameRules> gameRules,
      @Nullable Path tempDataPackDir
   );
}
