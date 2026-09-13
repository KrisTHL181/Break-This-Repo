package net.minecraft.client.gui.screens.options.controls;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ControlsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("controls.title");

   private static OptionInstance<?>[] options(Options p_342219_) {
      return new OptionInstance[]{
         p_342219_.toggleCrouch(),
         p_342219_.toggleSprint(),
         p_342219_.toggleAttack(),
         p_342219_.toggleUse(),
         p_342219_.autoJump(),
         p_342219_.sprintWindow(),
         p_342219_.operatorItemsTab()
      };
   }

   public ControlsScreen(Screen p_342882_, Options p_345081_) {
      super(p_342882_, p_345081_, TITLE);
   }

   @Override
   protected void addOptions() {
      this.list
         .addSmall(
            Button.builder(Component.translatable("options.mouse_settings"), p_344287_ -> this.minecraft.setScreen(new MouseSettingsScreen(this, this.options)))
               .build(),
            Button.builder(Component.translatable("controls.keybinds"), p_343299_ -> this.minecraft.setScreen(new KeyBindsScreen(this, this.options))).build()
         );
      this.list.addSmall(options(this.options));
   }
}
