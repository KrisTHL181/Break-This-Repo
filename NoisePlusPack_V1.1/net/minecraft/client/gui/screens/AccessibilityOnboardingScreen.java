package net.minecraft.client.gui.screens;

import com.mojang.text2speech.Narrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOnboardingScreen extends Screen {
   private static final Component TITLE = Component.translatable("accessibility.onboarding.screen.title");
   private static final Component ONBOARDING_NARRATOR_MESSAGE = Component.translatable("accessibility.onboarding.screen.narrator");
   private static final int PADDING = 4;
   private static final int TITLE_PADDING = 16;
   private static final float FADE_OUT_TIME = 1000.0F;
   private static final int TEXT_WIDGET_WIDTH = 374;
   private final LogoRenderer logoRenderer;
   private final Options options;
   private final boolean narratorAvailable;
   private boolean hasNarrated;
   private float timer;
   private final Runnable onClose;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, this.initTitleYPos(), 33);
   private float fadeInStart;
   private boolean fadingIn = true;
   private float fadeOutStart;

   public AccessibilityOnboardingScreen(Options p_265483_, Runnable p_298904_) {
      super(TITLE);
      this.options = p_265483_;
      this.onClose = p_298904_;
      this.logoRenderer = new LogoRenderer(true);
      this.narratorAvailable = Minecraft.getInstance().getNarrator().isActive();
   }

   @Override
   public void init() {
      LinearLayout linearlayout = this.layout.addToContents(LinearLayout.vertical());
      linearlayout.defaultCellSetting().alignHorizontallyCenter().padding(4);
      linearlayout.addChild(FocusableTextWidget.builder(this.title, this.font).maxWidth(374).build(), p_325362_ -> p_325362_.padding(8));
      if (this.options.narrator().createButton(this.options) instanceof CycleButton cyclebutton) {
         this.narratorButton = cyclebutton;
         this.narratorButton.active = this.narratorAvailable;
         linearlayout.addChild(this.narratorButton);
      }

      linearlayout.addChild(
         CommonButtons.accessibility(150, p_340778_ -> this.closeAndSetScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), false)
      );
      linearlayout.addChild(
         CommonButtons.language(
            150, p_340779_ -> this.closeAndSetScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), false
         )
      );
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_CONTINUE, p_267841_ -> this.onClose()).build());
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   protected void setInitialFocus() {
      if (this.narratorAvailable && this.narratorButton != null) {
         this.setInitialFocus(this.narratorButton);
      } else {
         super.setInitialFocus();
      }
   }

   private int initTitleYPos() {
      return 90;
   }

   @Override
   public void onClose() {
      if (this.fadeOutStart == 0.0F) {
         this.fadeOutStart = (float)Util.getMillis();
      }
   }

   private void closeAndSetScreen(Screen p_272914_) {
      this.close(false, () -> this.minecraft.setScreen(p_272914_));
   }

   private void close(boolean p_342115_, Runnable p_299263_) {
      if (p_342115_) {
         this.options.onboardingAccessibilityFinished();
      }

      Narrator.getNarrator().clear();
      p_299263_.run();
   }

   @Override
   public void render(GuiGraphics p_282353_, int p_265135_, int p_265032_, float p_265387_) {
      super.render(p_282353_, p_265135_, p_265032_, p_265387_);
      this.handleInitialNarrationDelay();
      if (this.fadeInStart == 0.0F && this.fadingIn) {
         this.fadeInStart = (float)Util.getMillis();
      }

      if (this.fadeInStart > 0.0F) {
         float f = ((float)Util.getMillis() - this.fadeInStart) / 2000.0F;
         float f1 = 1.0F;
         if (f >= 1.0F) {
            this.fadingIn = false;
            this.fadeInStart = 0.0F;
         } else {
            f = Mth.clamp(f, 0.0F, 1.0F);
            f1 = Mth.clampedMap(f, 0.5F, 1.0F, 0.0F, 1.0F);
         }

         this.fadeWidgets(f1);
      }

      if (this.fadeOutStart > 0.0F) {
         float f2 = 1.0F - ((float)Util.getMillis() - this.fadeOutStart) / 1000.0F;
         float f3 = 0.0F;
         if (f2 <= 0.0F) {
            this.fadeOutStart = 0.0F;
            this.close(true, this.onClose);
         } else {
            f2 = Mth.clamp(f2, 0.0F, 1.0F);
            f3 = Mth.clampedMap(f2, 0.5F, 1.0F, 0.0F, 1.0F);
         }

         this.fadeWidgets(f3);
      }

      this.logoRenderer.renderLogo(p_282353_, this.width, 1.0F);
   }

   @Override
   protected boolean panoramaShouldSpin() {
      return false;
   }

   private void handleInitialNarrationDelay() {
      if (!this.hasNarrated && this.narratorAvailable) {
         if (this.timer < 40.0F) {
            this.timer++;
         } else if (this.minecraft.isWindowActive()) {
            Narrator.getNarrator().say(ONBOARDING_NARRATOR_MESSAGE.getString(), true, this.minecraft.options.getFinalSoundSourceVolume(SoundSource.VOICE));
            this.hasNarrated = true;
         }
      }
   }
}
