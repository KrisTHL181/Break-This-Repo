package net.minecraft.client.renderer.texture;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class SimpleTexture extends ReloadableTexture {
   public SimpleTexture(final Identifier location) {
      super(location);
   }

   @Override
   public TextureContents loadContents(final ResourceManager resourceManager) throws IOException {
      return TextureContents.load(resourceManager, this.resourceId());
   }
}
