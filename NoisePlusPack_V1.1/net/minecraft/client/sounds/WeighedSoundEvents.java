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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   private final @Nullable Component subtitle;

   public WeighedSoundEvents(Identifier p_454312_, @Nullable String p_120447_) {
      if (SharedConstants.DEBUG_SUBTITLES) {
         MutableComponent mutablecomponent = Component.literal(p_454312_.getPath());
         if ("FOR THE DEBUG!".equals(p_120447_)) {
            mutablecomponent = mutablecomponent.append(Component.literal(" missing").withStyle(ChatFormatting.RED));
         }

         this.subtitle = mutablecomponent;
      } else {
         this.subtitle = p_120447_ == null ? null : Component.translatable(p_120447_);
      }
   }

   @Override
   public int getWeight() {
      int i = 0;

      for (Weighted<Sound> weighted : this.list) {
         i += weighted.getWeight();
      }

      return i;
   }

   public Sound getSound(RandomSource p_235265_) {
      int i = this.getWeight();
      if (!this.list.isEmpty() && i != 0) {
         int j = p_235265_.nextInt(i);

         for (Weighted<Sound> weighted : this.list) {
            j -= weighted.getWeight();
            if (j < 0) {
               return weighted.getSound(p_235265_);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> p_120452_) {
      this.list.add(p_120452_);
   }

   public @Nullable Component getSubtitle() {
      return this.subtitle;
   }

   @Override
   public void preloadIfRequired(SoundEngine p_120450_) {
      for (Weighted<Sound> weighted : this.list) {
         weighted.preloadIfRequired(p_120450_);
      }
   }
}
