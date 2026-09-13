package net.minecraft.client.renderer;

import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

public record SpriteMapper(Identifier sheet, String prefix) {
   public SpriteId apply(final Identifier path) {
      return new SpriteId(this.sheet, path.withPrefix(this.prefix + "/"));
   }

   public SpriteId defaultNamespaceApply(final String path) {
      return this.apply(Identifier.withDefaultNamespace(path));
   }
}
