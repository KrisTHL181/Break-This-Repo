package net.minecraft.server.permissions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public class PermissionTypes {
   public static MapCodec<? extends Permission> bootstrap(Registry<MapCodec<? extends Permission>> p_454344_) {
      Registry.register(p_454344_, Identifier.withDefaultNamespace("atom"), Permission.Atom.MAP_CODEC);
      return Registry.register(p_454344_, Identifier.withDefaultNamespace("command_level"), Permission.HasCommandLevel.MAP_CODEC);
   }
}
