package net.minecraft.server.permissions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;

public interface PermissionCheck {
   Codec<PermissionCheck> CODEC = BuiltInRegistries.PERMISSION_CHECK_TYPE.byNameCodec().dispatch(PermissionCheck::codec, p_456900_ -> p_456900_);

   boolean check(PermissionSet var1);

   MapCodec<? extends PermissionCheck> codec();

   class AlwaysPass implements PermissionCheck {
      public static final PermissionCheck.AlwaysPass INSTANCE = new PermissionCheck.AlwaysPass();
      public static final MapCodec<PermissionCheck.AlwaysPass> MAP_CODEC = MapCodec.unit(INSTANCE);

      private AlwaysPass() {
      }

      @Override
      public boolean check(PermissionSet p_455988_) {
         return true;
      }

      @Override
      public MapCodec<PermissionCheck.AlwaysPass> codec() {
         return MAP_CODEC;
      }
   }

   record Require(Permission permission) implements PermissionCheck {
      public static final MapCodec<PermissionCheck.Require> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_457074_ -> p_457074_.group(Permission.CODEC.fieldOf("permission").forGetter(PermissionCheck.Require::permission))
            .apply(p_457074_, PermissionCheck.Require::new)
      );

      @Override
      public MapCodec<PermissionCheck.Require> codec() {
         return MAP_CODEC;
      }

      @Override
      public boolean check(PermissionSet p_457403_) {
         return p_457403_.hasPermission(this.permission);
      }
   }
}
