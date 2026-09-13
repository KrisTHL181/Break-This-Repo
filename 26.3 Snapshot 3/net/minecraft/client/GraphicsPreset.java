package net.minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.serialization.Codec;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public enum GraphicsPreset implements StringRepresentable {
   FAST("fast", "options.graphics.fast"),
   FANCY("fancy", "options.graphics.fancy"),
   FABULOUS("fabulous", "options.graphics.fabulous"),
   CUSTOM("custom", "options.graphics.custom");

   private final String serializedName;
   private final String key;
   public static final Codec<GraphicsPreset> CODEC = StringRepresentable.fromEnum(GraphicsPreset::values);

   GraphicsPreset(final String serializedName, final String key) {
      this.serializedName = serializedName;
      this.key = key;
   }

   @Override
   public String getSerializedName() {
      return this.serializedName;
   }

   public String getKey() {
      return this.key;
   }

   public void apply(final Minecraft minecraft) {
      OptionsSubScreen screen = minecraft.gui != null && minecraft.gui.screen() instanceof OptionsSubScreen ? (OptionsSubScreen)minecraft.gui.screen() : null;
      GpuDevice device = RenderSystem.getDevice();
      switch (this) {
         case FAST: {
            int viewDistance = 8;
            set(screen, minecraft.options.biomeBlendRadius(), 1);
            set(screen, minecraft.options.renderDistance(), 8);
            set(screen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.NONE);
            set(screen, minecraft.options.simulationDistance(), 6);
            set(screen, minecraft.options.ambientOcclusion(), false);
            set(screen, minecraft.options.cloudStatus(), CloudStatus.FAST);
            set(screen, minecraft.options.particles(), ParticleStatus.DECREASED);
            set(screen, minecraft.options.mipmapLevels(), 2);
            set(screen, minecraft.options.entityShadows(), false);
            set(screen, minecraft.options.entityDistanceScaling(), 0.75);
            set(screen, minecraft.options.menuBackgroundBlurriness(), 2);
            set(screen, minecraft.options.cloudRange(), 32);
            set(screen, minecraft.options.cutoutLeaves(), false);
            set(screen, minecraft.options.improvedTransparency(), false);
            set(screen, minecraft.options.weatherRadius(), 5);
            set(screen, minecraft.options.maxAnisotropyBit(), 1);
            set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.NONE);
            break;
         }
         case FANCY: {
            int viewDistance = 16;
            set(screen, minecraft.options.biomeBlendRadius(), 2);
            set(screen, minecraft.options.renderDistance(), 16);
            set(screen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
            set(screen, minecraft.options.simulationDistance(), 12);
            set(screen, minecraft.options.ambientOcclusion(), true);
            set(screen, minecraft.options.cloudStatus(), CloudStatus.FANCY);
            set(screen, minecraft.options.particles(), ParticleStatus.ALL);
            set(screen, minecraft.options.mipmapLevels(), 4);
            set(screen, minecraft.options.entityShadows(), true);
            set(screen, minecraft.options.entityDistanceScaling(), 1.0);
            set(screen, minecraft.options.menuBackgroundBlurriness(), 5);
            set(screen, minecraft.options.cloudRange(), 64);
            set(screen, minecraft.options.cutoutLeaves(), true);
            set(screen, minecraft.options.improvedTransparency(), false);
            set(screen, minecraft.options.weatherRadius(), 10);
            set(screen, minecraft.options.maxAnisotropyBit(), 1);
            set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.RGSS);
            break;
         }
         case FABULOUS: {
            int viewDistance = 32;
            set(screen, minecraft.options.biomeBlendRadius(), 2);
            set(screen, minecraft.options.renderDistance(), 32);
            set(screen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
            set(screen, minecraft.options.simulationDistance(), 12);
            set(screen, minecraft.options.ambientOcclusion(), true);
            set(screen, minecraft.options.cloudStatus(), CloudStatus.FANCY);
            set(screen, minecraft.options.particles(), ParticleStatus.ALL);
            set(screen, minecraft.options.mipmapLevels(), 4);
            set(screen, minecraft.options.entityShadows(), true);
            set(screen, minecraft.options.entityDistanceScaling(), 1.25);
            set(screen, minecraft.options.menuBackgroundBlurriness(), 5);
            set(screen, minecraft.options.cloudRange(), 128);
            set(screen, minecraft.options.cutoutLeaves(), true);
            set(screen, minecraft.options.improvedTransparency(), true);
            set(screen, minecraft.options.weatherRadius(), 10);
            set(screen, minecraft.options.maxAnisotropyBit(), 2);
            if (device.getDeviceInfo().hintsAndWorkarounds().anisotropyHasKnownIssues()) {
               set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.RGSS);
            } else {
               set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.ANISOTROPIC);
            }
         }
      }
   }

   private static <T> void set(final @Nullable OptionsSubScreen screen, final OptionInstance<T> option, final T value) {
      if (option.get() != value) {
         option.set(value);
         if (screen != null) {
            screen.resetOption(option);
         }
      }
   }
}
