package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   private final @Nullable Component subtitle;

   public WeighedSoundEvents(final Identifier location, final @Nullable String subtitle) {
      if (SharedConstants.DEBUG_SUBTITLES) {
         MutableComponent components = Component.literal(location.getPath());
         if ("FOR THE DEBUG!".equals(subtitle)) {
            components = components.append(Component.literal(" missing").withStyle(ChatFormatting.RED));
         }

         this.subtitle = components;
      } else {
         this.subtitle = subtitle == null ? null : Component.translatable(subtitle);
      }
   }

   @Override
   public int getWeight() {
      int sum = 0;

      for (Weighted<Sound> sound : this.list) {
         sum += sound.getWeight();
      }

      return sum;
   }

   public Sound getSound(final RandomSource random) {
      int weight = this.getWeight();
      if (!this.list.isEmpty() && weight != 0) {
         int index = random.nextInt(weight);

         for (Weighted<Sound> weighted : this.list) {
            index -= weighted.getWeight();
            if (index < 0) {
               return weighted.getSound(random);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(final Weighted<Sound> sound) {
      this.list.add(sound);
   }

   public @Nullable Component getSubtitle() {
      return this.subtitle;
   }

   @Override
   public void preloadIfRequired(final SoundEngine soundEngine) {
      for (Weighted<Sound> weighted : this.list) {
         weighted.preloadIfRequired(soundEngine);
      }
   }
}
