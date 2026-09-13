package net.minecraft.client.gui.screens.options;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MouseSettingsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.mouse_settings.title");

   private static OptionInstance<?>[] options(Options p_344227_) {
      return new OptionInstance[]{
         p_344227_.sensitivity(),
         p_344227_.touchscreen(),
         p_344227_.mouseWheelSensitivity(),
         p_344227_.discreteMouseScroll(),
         p_344227_.invertMouseX(),
         p_344227_.invertMouseY(),
         p_344227_.allowCursorChanges()
      };
   }

   public MouseSettingsScreen(Screen p_342435_, Options p_344636_) {
      super(p_342435_, p_344636_, TITLE);
   }

   @Override
   protected void addOptions() {
      if (InputConstants.isRawMouseInputSupported()) {
         this.list.addSmall(Stream.concat(Arrays.stream(options(this.options)), Stream.of(this.options.rawMouseInput())).toArray(OptionInstance[]::new));
      } else {
         this.list.addSmall(options(this.options));
      }
   }
}
