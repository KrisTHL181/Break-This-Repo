package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface WorldCreationContextMapper {
   WorldCreationContext apply(ReloadableServerResources var1, LayeredRegistryAccess<RegistryLayer> var2, DataPackReloadCookie var3);
}
