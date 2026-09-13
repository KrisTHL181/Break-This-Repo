package net.minecraft.client.gui.screens.options;

import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.flag.FeatureFlags;

public class AccessibilityOptionsScreen extends OptionsSubScreen {
   public static final Component TITLE = Component.translatable("options.accessibility.title");

   private static OptionInstance<?>[] options(final Options options) {
      return new OptionInstance[]{
         options.narrator(),
         options.showSubtitles(),
         options.highContrast(),
         options.menuBackgroundBlurriness(),
         options.textBackgroundOpacity(),
         options.backgroundForChatOnly(),
         options.chatOpacity(),
         options.chatLineSpacing(),
         options.chatDelay(),
         options.notificationDisplayTime(),
         options.bobView(),
         options.screenEffectScale(),
         options.fovEffectScale(),
         options.darknessEffectScale(),
         options.damageTiltStrength(),
         options.glintSpeed(),
         options.glintStrength(),
         options.hideLightningFlash(),
         options.darkMojangStudiosBackground(),
         options.panoramaSpeed(),
         options.hideSplashTexts(),
         options.narratorHotkey(),
         options.rotateWithMinecart(),
         options.highContrastBlockOutline()
      };
   }

   public AccessibilityOptionsScreen(final Screen lastScreen, final Options options) {
      super(lastScreen, options, TITLE);
   }

   @Override
   protected void init() {
      super.init();
      AbstractWidget highContrast = this.list.findOption(this.options.highContrast());
      if (highContrast != null && !this.minecraft.getResourcePackRepository().getAvailableIds().contains("high_contrast")) {
         highContrast.active = false;
         highContrast.setTooltip(Tooltip.create(Component.translatable("options.accessibility.high_contrast.error.tooltip")));
      }

      AbstractWidget rotateWithMinecart = this.list.findOption(this.options.rotateWithMinecart());
      if (rotateWithMinecart != null) {
         rotateWithMinecart.active = this.isMinecartOptionEnabled();
      }
   }

   @Override
   protected void addOptions() {
      OptionInstance<?>[] optionsInstances = options(this.options);
      Button controlsLink = Button.builder(OptionsScreen.CONTROLS, var1x -> this.minecraft.gui.setScreen(new ControlsScreen(this, this.options))).build();
      OptionInstance<?> firstOptionInstance = optionsInstances[0];
      this.list.addSmall(firstOptionInstance.createButton(this.options), this.options.narrator(), controlsLink);
      this.list.addSmall(Arrays.stream(optionsInstances).filter(instance -> instance != firstOptionInstance).toArray(OptionInstance[]::new));
   }

   @Override
   protected void addFooter() {
      LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footer.addChild(
         Button.builder(Component.translatable("options.accessibility.link"), ConfirmLinkScreen.confirmLink(this, CommonLinks.ACCESSIBILITY_HELP)).build()
      );
      footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.minecraft.gui.setScreen(this.lastScreen)).build());
   }

   private boolean isMinecartOptionEnabled() {
      return this.minecraft.level != null && this.minecraft.level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
   }
}
