package net.minecraft.server.permissions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public class PermissionCheckTypes {
   public static MapCodec<? extends PermissionCheck> bootstrap(final Registry<MapCodec<? extends PermissionCheck>> registry) {
      Registry.register(registry, Identifier.withDefaultNamespace("always_pass"), PermissionCheck.AlwaysPass.MAP_CODEC);
      return Registry.register(registry, Identifier.withDefaultNamespace("require"), PermissionCheck.Require.MAP_CODEC);
   }
}
