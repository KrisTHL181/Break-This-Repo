package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;

@FunctionalInterface
public interface WorldCreationContextMapper {
   WorldCreationContext apply(
      final ReloadableServerResources managers, final LayeredRegistryAccess<RegistryLayer> registries, final DataPackReloadCookie cookie
   );
}
