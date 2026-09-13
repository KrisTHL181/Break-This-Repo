package net.minecraft.server.permissions;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public interface Permission {
   Codec<Permission> FULL_CODEC = BuiltInRegistries.PERMISSION_TYPE.byNameCodec().dispatch(Permission::codec, p_450483_ -> p_450483_);
   Codec<Permission> CODEC = Codec.either(FULL_CODEC, Identifier.CODEC)
      .xmap(
         p_460185_ -> (Permission)p_460185_.map(p_456763_ -> p_456763_, Permission.Atom::create),
         p_451793_ -> p_451793_ instanceof Permission.Atom permission$atom ? Either.right(permission$atom.id()) : Either.left(p_451793_)
      );

   MapCodec<? extends Permission> codec();

   record Atom(Identifier id) implements Permission {
      public static final MapCodec<Permission.Atom> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_454569_ -> p_454569_.group(Identifier.CODEC.fieldOf("id").forGetter(Permission.Atom::id)).apply(p_454569_, Permission.Atom::new)
      );

      @Override
      public MapCodec<Permission.Atom> codec() {
         return MAP_CODEC;
      }

      public static Permission.Atom create(String p_460355_) {
         return create(Identifier.withDefaultNamespace(p_460355_));
      }

      public static Permission.Atom create(Identifier p_452066_) {
         return new Permission.Atom(p_452066_);
      }
   }

   record HasCommandLevel(PermissionLevel level) implements Permission {
      public static final MapCodec<Permission.HasCommandLevel> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_456445_ -> p_456445_.group(PermissionLevel.CODEC.fieldOf("level").forGetter(Permission.HasCommandLevel::level))
            .apply(p_456445_, Permission.HasCommandLevel::new)
      );

      @Override
      public MapCodec<Permission.HasCommandLevel> codec() {
         return MAP_CODEC;
      }
   }
}
