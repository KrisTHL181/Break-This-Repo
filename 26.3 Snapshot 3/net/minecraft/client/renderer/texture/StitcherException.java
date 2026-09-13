package net.minecraft.client.renderer.texture;

import java.util.Collection;
import java.util.Locale;

public class StitcherException extends RuntimeException {
   private final Collection<Stitcher.Entry> allSprites;

   public StitcherException(final Stitcher.Entry sprite, final Collection<Stitcher.Entry> allSprites) {
      super(
         String.format(
            Locale.ROOT, "Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?", sprite.name(), sprite.width(), sprite.height()
         )
      );
      this.allSprites = allSprites;
   }

   public Collection<Stitcher.Entry> getAllSprites() {
      return this.allSprites;
   }
}
