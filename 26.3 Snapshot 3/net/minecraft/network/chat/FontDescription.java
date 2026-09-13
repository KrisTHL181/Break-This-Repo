package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;

public interface FontDescription {
   Codec<FontDescription> CODEC = Identifier.CODEC
      .flatComapMap(
         FontDescription.Resource::new,
         fontDescription -> fontDescription instanceof FontDescription.Resource resource
            ? DataResult.success(resource.id())
            : DataResult.error(() -> "Unsupported font description type: " + fontDescription)
      );
   FontDescription.Resource DEFAULT = new FontDescription.Resource(Identifier.withDefaultNamespace("default"));

   record AtlasSprite(Identifier atlasId, Identifier spriteId) implements FontDescription {
   }

   record PlayerSprite(ResolvableProfile profile, boolean hat) implements FontDescription {
   }

   record Resource(Identifier id) implements FontDescription {
   }
}
