package net.minecraft.client.renderer.texture;

import java.util.Collection;
import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StitcherException extends RuntimeException {
   private final Collection<Stitcher.Entry> allSprites;

   public StitcherException(Stitcher.Entry p_250177_, Collection<Stitcher.Entry> p_248618_) {
      super(
         String.format(
            Locale.ROOT,
            "Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?",
            p_250177_.name(),
            p_250177_.width(),
            p_250177_.height()
         )
      );
      this.allSprites = p_248618_;
   }

   public Collection<Stitcher.Entry> getAllSprites() {
      return this.allSprites;
   }
}
