package net.minecraft.client.gui.screens.options;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MouseSettingsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.mouse_settings.title");

   private static OptionInstance<?>[] options(final Options options) {
      return new OptionInstance[]{
         options.sensitivity(),
         options.mouseWheelSensitivity(),
         options.discreteMouseScroll(),
         options.invertMouseX(),
         options.invertMouseY(),
         options.allowCursorChanges()
      };
   }

   public MouseSettingsScreen(final Screen lastScreen, final Options options) {
      super(lastScreen, options, TITLE);
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
