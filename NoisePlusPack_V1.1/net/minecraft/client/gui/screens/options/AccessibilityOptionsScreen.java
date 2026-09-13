package net.minecraft.client.gui.screens.options;

import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOptionsScreen extends OptionsSubScreen {
   public static final Component TITLE = Component.translatable("options.accessibility.title");

   private static OptionInstance<?>[] options(Options p_343652_) {
      return new OptionInstance[]{
         p_343652_.narrator(),
         p_343652_.showSubtitles(),
         p_343652_.highContrast(),
         p_343652_.menuBackgroundBlurriness(),
         p_343652_.textBackgroundOpacity(),
         p_343652_.backgroundForChatOnly(),
         p_343652_.chatOpacity(),
         p_343652_.chatLineSpacing(),
         p_343652_.chatDelay(),
         p_343652_.notificationDisplayTime(),
         p_343652_.bobView(),
         p_343652_.screenEffectScale(),
         p_343652_.fovEffectScale(),
         p_343652_.darknessEffectScale(),
         p_343652_.damageTiltStrength(),
         p_343652_.glintSpeed(),
         p_343652_.glintStrength(),
         p_343652_.hideLightningFlash(),
         p_343652_.darkMojangStudiosBackground(),
         p_343652_.panoramaSpeed(),
         p_343652_.hideSplashTexts(),
         p_343652_.narratorHotkey(),
         p_343652_.rotateWithMinecart(),
         p_343652_.highContrastBlockOutline()
      };
   }

   public AccessibilityOptionsScreen(Screen p_343335_, Options p_343534_) {
      super(p_343335_, p_343534_, TITLE);
   }

   @Override
   protected void init() {
      super.init();
      AbstractWidget abstractwidget = this.list.findOption(this.options.highContrast());
      if (abstractwidget != null && !this.minecraft.getResourcePackRepository().getAvailableIds().contains("high_contrast")) {
         abstractwidget.active = false;
         abstractwidget.setTooltip(Tooltip.create(Component.translatable("options.accessibility.high_contrast.error.tooltip")));
      }

      AbstractWidget abstractwidget1 = this.list.findOption(this.options.rotateWithMinecart());
      if (abstractwidget1 != null) {
         abstractwidget1.active = this.isMinecartOptionEnabled();
      }
   }

   @Override
   protected void addOptions() {
      OptionInstance<?>[] optioninstance = options(this.options);
      Button button = Button.builder(OptionsScreen.CONTROLS, p_420764_ -> this.minecraft.setScreen(new ControlsScreen(this, this.options))).build();
      OptionInstance<?> optioninstance1 = optioninstance[0];
      this.list.addSmall(optioninstance1.createButton(this.options), this.options.narrator(), button);
      this.list.addSmall(Arrays.stream(optioninstance).filter(p_420763_ -> p_420763_ != optioninstance1).toArray(OptionInstance[]::new));
   }

   @Override
   protected void addFooter() {
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearlayout.addChild(
         Button.builder(Component.translatable("options.accessibility.link"), ConfirmLinkScreen.confirmLink(this, CommonLinks.ACCESSIBILITY_HELP)).build()
      );
      linearlayout.addChild(Button.builder(CommonComponents.GUI_DONE, p_343568_ -> this.minecraft.setScreen(this.lastScreen)).build());
   }

   @Override
   protected boolean panoramaShouldSpin() {
      return !(this.lastScreen instanceof AccessibilityOnboardingScreen);
   }

   private boolean isMinecartOptionEnabled() {
      return this.minecraft.level != null && this.minecraft.level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
   }
}
