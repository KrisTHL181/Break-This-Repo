package net.minecraft.server.permissions;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public interface Permission {
   Codec<Permission> FULL_CODEC = BuiltInRegistries.PERMISSION_TYPE.byNameCodec().dispatch(Permission::codec, c -> c);
   Codec<Permission> CODEC = Codec.either(FULL_CODEC, Identifier.CODEC)
      .xmap(
         e -> (Permission)e.map(permission -> permission, Permission.Atom::create),
         permission -> permission instanceof Permission.Atom atom ? Either.right(atom.id()) : Either.left(permission)
      );

   MapCodec<? extends Permission> codec();

   record Atom(Identifier id) implements Permission {
      public static final MapCodec<Permission.Atom> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Identifier.CODEC.fieldOf("id").forGetter(Permission.Atom::id)).apply(i, Permission.Atom::new)
      );

      @Override
      public MapCodec<Permission.Atom> codec() {
         return MAP_CODEC;
      }

      public static Permission.Atom create(final String name) {
         return create(Identifier.withDefaultNamespace(name));
      }

      public static Permission.Atom create(final Identifier id) {
         return new Permission.Atom(id);
      }
   }

   record HasCommandLevel(PermissionLevel level) implements Permission {
      public static final MapCodec<Permission.HasCommandLevel> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(PermissionLevel.CODEC.fieldOf("level").forGetter(Permission.HasCommandLevel::level)).apply(i, Permission.HasCommandLevel::new)
      );

      @Override
      public MapCodec<Permission.HasCommandLevel> codec() {
         return MAP_CODEC;
      }
   }
}
