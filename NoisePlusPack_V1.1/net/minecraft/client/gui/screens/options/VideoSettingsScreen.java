package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VideoSettingsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.videoTitle");
   private static final Component IMPROVED_TRANSPARENCY = Component.translatable("options.improvedTransparency").withStyle(ChatFormatting.ITALIC);
   private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", IMPROVED_TRANSPARENCY, IMPROVED_TRANSPARENCY);
   private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
   private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
   private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
   private static final Component DISPLAY_HEADER = Component.translatable("options.video.display.header");
   private static final Component QUALITY_HEADER = Component.translatable("options.video.quality.header");
   private static final Component PREFERENCES_HEADER = Component.translatable("options.video.preferences.header");
   private final GpuWarnlistManager gpuWarnlistManager;
   private final int oldMipmaps;
   private final int oldAnisotropyBit;
   private final TextureFilteringMethod oldTextureFiltering;

   private static OptionInstance<?>[] qualityOptions(Options p_460714_) {
      return new OptionInstance[]{
         p_460714_.biomeBlendRadius(),
         p_460714_.renderDistance(),
         p_460714_.prioritizeChunkUpdates(),
         p_460714_.simulationDistance(),
         p_460714_.ambientOcclusion(),
         p_460714_.cloudStatus(),
         p_460714_.particles(),
         p_460714_.mipmapLevels(),
         p_460714_.entityShadows(),
         p_460714_.entityDistanceScaling(),
         p_460714_.menuBackgroundBlurriness(),
         p_460714_.cloudRange(),
         p_460714_.cutoutLeaves(),
         p_460714_.improvedTransparency(),
         p_460714_.textureFiltering(),
         p_460714_.maxAnisotropyBit(),
         p_460714_.weatherRadius()
      };
   }

   private static OptionInstance<?>[] displayOptions(Options p_455698_) {
      return new OptionInstance[]{
         p_455698_.framerateLimit(), p_455698_.enableVsync(), p_455698_.inactivityFpsLimit(), p_455698_.guiScale(), p_455698_.fullscreen(), p_455698_.gamma()
      };
   }

   private static OptionInstance<?>[] preferenceOptions(Options p_459591_) {
      return new OptionInstance[]{p_459591_.showAutosaveIndicator(), p_459591_.vignette(), p_459591_.attackIndicator(), p_459591_.chunkSectionFadeInTime()};
   }

   public VideoSettingsScreen(Screen p_342724_, Minecraft p_343064_, Options p_343837_) {
      super(p_342724_, p_343837_, TITLE);
      this.gpuWarnlistManager = p_343064_.getGpuWarnlistManager();
      this.gpuWarnlistManager.resetWarnings();
      if (p_343837_.improvedTransparency().get()) {
         this.gpuWarnlistManager.dismissWarning();
      }

      this.oldMipmaps = p_343837_.mipmapLevels().get();
      this.oldAnisotropyBit = p_343837_.maxAnisotropyBit().get();
      this.oldTextureFiltering = p_343837_.textureFiltering().get();
   }

   @Override
   protected void addOptions() {
      int i = -1;
      Window window = this.minecraft.getWindow();
      Monitor monitor = window.findBestMonitor();
      int j;
      if (monitor == null) {
         j = -1;
      } else {
         Optional<VideoMode> optional = window.getPreferredFullscreenVideoMode();
         j = optional.map(monitor::getVideoModeIndex).orElse(-1);
      }

      OptionInstance<Integer> optioninstance = new OptionInstance<>(
         "options.fullscreen.resolution",
         OptionInstance.noTooltip(),
         (p_344242_, p_344033_) -> {
            if (monitor == null) {
               return Component.translatable("options.fullscreen.unavailable");
            }

            if (p_344033_ == -1) {
               return Options.genericValueLabel(p_344242_, Component.translatable("options.fullscreen.current"));
            }

            VideoMode videomode = monitor.getMode(p_344033_);
            return Options.genericValueLabel(
               p_344242_,
               Component.translatable(
                  "options.fullscreen.entry",
                  videomode.getWidth(),
                  videomode.getHeight(),
                  videomode.getRefreshRate(),
                  videomode.getRedBits() + videomode.getGreenBits() + videomode.getBlueBits()
               )
            );
         },
         new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1),
         j,
         p_345267_ -> {
            if (monitor != null) {
               window.setPreferredFullscreenVideoMode(p_345267_ == -1 ? Optional.empty() : Optional.of(monitor.getMode(p_345267_)));
            }
         }
      );
      this.list.addHeader(DISPLAY_HEADER);
      this.list.addBig(optioninstance);
      this.list.addSmall(displayOptions(this.options));
      this.list.addHeader(QUALITY_HEADER);
      this.list.addBig(this.options.graphicsPreset());
      this.list.addSmall(qualityOptions(this.options));
      this.list.addHeader(PREFERENCES_HEADER);
      this.list.addSmall(preferenceOptions(this.options));
   }

   @Override
   public void tick() {
      if (this.list != null && this.list.findOption(this.options.maxAnisotropyBit()) instanceof AbstractSliderButton abstractsliderbutton) {
         abstractsliderbutton.active = this.options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC;
      }

      super.tick();
   }

   @Override
   public void onClose() {
      this.minecraft.getWindow().changeFullscreenVideoMode();
      super.onClose();
   }

   @Override
   public void removed() {
      if (this.options.mipmapLevels().get() != this.oldMipmaps
         || this.options.maxAnisotropyBit().get() != this.oldAnisotropyBit
         || this.options.textureFiltering().get() != this.oldTextureFiltering) {
         this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
         this.minecraft.delayTextureReload();
      }

      super.removed();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_428387_, boolean p_424596_) {
      if (super.mouseClicked(p_428387_, p_424596_)) {
         if (this.gpuWarnlistManager.isShowingWarning()) {
            List<Component> list = Lists.newArrayList(new Component[]{WARNING_MESSAGE, CommonComponents.NEW_LINE});
            String s = this.gpuWarnlistManager.getRendererWarnings();
            if (s != null) {
               list.add(CommonComponents.NEW_LINE);
               list.add(Component.translatable("options.graphics.warning.renderer", s).withStyle(ChatFormatting.GRAY));
            }

            String s1 = this.gpuWarnlistManager.getVendorWarnings();
            if (s1 != null) {
               list.add(CommonComponents.NEW_LINE);
               list.add(Component.translatable("options.graphics.warning.vendor", s1).withStyle(ChatFormatting.GRAY));
            }

            String s2 = this.gpuWarnlistManager.getVersionWarnings();
            if (s2 != null) {
               list.add(CommonComponents.NEW_LINE);
               list.add(Component.translatable("options.graphics.warning.version", s2).withStyle(ChatFormatting.GRAY));
            }

            this.minecraft
               .setScreen(
                  new UnsupportedGraphicsWarningScreen(
                     WARNING_TITLE, list, ImmutableList.of(new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_ACCEPT, p_448063_ -> {
                        this.options.improvedTransparency().set(true);
                        Minecraft.getInstance().levelRenderer.allChanged();
                        this.gpuWarnlistManager.dismissWarning();
                        this.minecraft.setScreen(this);
                     }), new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_CANCEL, p_448064_ -> {
                        this.gpuWarnlistManager.dismissWarning();
                        this.options.improvedTransparency().set(false);
                        this.updateTransparencyButton();
                        this.minecraft.setScreen(this);
                     }))
                  )
               );
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double p_345374_, double p_345119_, double p_345124_, double p_343217_) {
      if (this.minecraft.hasControlDown()) {
         OptionInstance<Integer> optioninstance = this.options.guiScale();
         if (optioninstance.values() instanceof OptionInstance.ClampingLazyMaxIntRange optioninstance$clampinglazymaxintrange) {
            int k = optioninstance.get();
            int i = k == 0 ? optioninstance$clampinglazymaxintrange.maxInclusive() + 1 : k;
            int j = i + (int)Math.signum(p_343217_);
            if (j != 0 && j <= optioninstance$clampinglazymaxintrange.maxInclusive() && j >= optioninstance$clampinglazymaxintrange.minInclusive()) {
               CycleButton<Integer> cyclebutton = (CycleButton<Integer>)this.list.findOption(optioninstance);
               if (cyclebutton != null) {
                  optioninstance.set(j);
                  cyclebutton.setValue(j);
                  this.list.setScrollAmount(0.0);
                  return true;
               }
            }
         }

         return false;
      } else {
         return super.mouseScrolled(p_345374_, p_345119_, p_345124_, p_343217_);
      }
   }

   public void updateFullscreenButton(boolean p_397133_) {
      if (this.list != null) {
         AbstractWidget abstractwidget = this.list.findOption(this.options.fullscreen());
         if (abstractwidget != null) {
            CycleButton<Boolean> cyclebutton = (CycleButton<Boolean>)abstractwidget;
            cyclebutton.setValue(p_397133_);
         }
      }
   }

   public void updateTransparencyButton() {
      if (this.list != null) {
         OptionInstance<Boolean> optioninstance = this.options.improvedTransparency();
         AbstractWidget abstractwidget = this.list.findOption(optioninstance);
         if (abstractwidget != null) {
            CycleButton<Boolean> cyclebutton = (CycleButton<Boolean>)abstractwidget;
            cyclebutton.setValue(optioninstance.get());
         }
      }
   }
}
