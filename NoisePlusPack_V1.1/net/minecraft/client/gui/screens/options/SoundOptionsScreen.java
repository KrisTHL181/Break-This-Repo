package net.minecraft.client.gui.screens.options;

import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundOptionsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.sounds.title");

   public SoundOptionsScreen(Screen p_343471_, Options p_344842_) {
      super(p_343471_, p_344842_, TITLE);
   }

   @Override
   protected void addOptions() {
      this.list.addBig(this.options.getSoundSourceOptionInstance(SoundSource.MASTER));
      this.list.addSmall(this.getAllSoundOptionsExceptMaster());
      this.list.addBig(this.options.soundDevice());
      this.list.addSmall(this.options.showSubtitles(), this.options.directionalAudio());
      this.list.addSmall(this.options.musicFrequency(), this.options.musicToast());
   }

   private OptionInstance<?>[] getAllSoundOptionsExceptMaster() {
      return Arrays.stream(SoundSource.values())
         .filter(p_343395_ -> p_343395_ != SoundSource.MASTER)
         .map(this.options::getSoundSourceOptionInstance)
         .toArray(OptionInstance[]::new);
   }
}
