package net.minecraft.client.resources.sounds;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SoundEventRegistration {
   private final List<Sound> sounds;
   private final boolean replace;
   private final @Nullable String subtitle;

   public SoundEventRegistration(List<Sound> p_119819_, boolean p_119820_, @Nullable String p_119821_) {
      this.sounds = p_119819_;
      this.replace = p_119820_;
      this.subtitle = p_119821_;
   }

   public List<Sound> getSounds() {
      return this.sounds;
   }

   public boolean isReplace() {
      return this.replace;
   }

   public @Nullable String getSubtitle() {
      return this.subtitle;
   }
}
