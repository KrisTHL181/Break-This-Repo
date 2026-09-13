package net.minecraft.client.gui.screens.worldselection;

import java.nio.file.Path;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface CreateWorldCallback {
   boolean create(CreateWorldScreen var1, LayeredRegistryAccess<RegistryLayer> var2, PrimaryLevelData var3, @Nullable Path var4);
}
