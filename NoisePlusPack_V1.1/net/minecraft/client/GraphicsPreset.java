package net.minecraft.client;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public enum GraphicsPreset implements StringRepresentable {
   FAST("fast", "options.graphics.fast"),
   FANCY("fancy", "options.graphics.fancy"),
   FABULOUS("fabulous", "options.graphics.fabulous"),
   CUSTOM("custom", "options.graphics.custom");

   private final String serializedName;
   private final String key;
   public static final Codec<GraphicsPreset> CODEC = StringRepresentable.fromEnum(GraphicsPreset::values);

   GraphicsPreset(final String p_457983_, final String p_458839_) {
      this.serializedName = p_457983_;
      this.key = p_458839_;
   }

   @Override
   public String getSerializedName() {
      return this.serializedName;
   }

   public String getKey() {
      return this.key;
   }

   public void apply(Minecraft p_456789_) {
      OptionsSubScreen optionssubscreen = p_456789_.screen instanceof OptionsSubScreen ? (OptionsSubScreen)p_456789_.screen : null;
      GpuDevice gpudevice = RenderSystem.getDevice();
      switch (this) {
         case FAST:
            int k = 8;
            this.set(optionssubscreen, p_456789_.options.biomeBlendRadius(), 1);
            this.set(optionssubscreen, p_456789_.options.renderDistance(), 8);
            this.set(optionssubscreen, p_456789_.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.NONE);
            this.set(optionssubscreen, p_456789_.options.simulationDistance(), 6);
            this.set(optionssubscreen, p_456789_.options.ambientOcclusion(), false);
            this.set(optionssubscreen, p_456789_.options.cloudStatus(), CloudStatus.FAST);
            this.set(optionssubscreen, p_456789_.options.particles(), ParticleStatus.DECREASED);
            this.set(optionssubscreen, p_456789_.options.mipmapLevels(), 2);
            this.set(optionssubscreen, p_456789_.options.entityShadows(), false);
            this.set(optionssubscreen, p_456789_.options.entityDistanceScaling(), 0.75);
            this.set(optionssubscreen, p_456789_.options.menuBackgroundBlurriness(), 2);
            this.set(optionssubscreen, p_456789_.options.cloudRange(), 32);
            this.set(optionssubscreen, p_456789_.options.cutoutLeaves(), false);
            this.set(optionssubscreen, p_456789_.options.improvedTransparency(), false);
            this.set(optionssubscreen, p_456789_.options.weatherRadius(), 5);
            this.set(optionssubscreen, p_456789_.options.maxAnisotropyBit(), 1);
            this.set(optionssubscreen, p_456789_.options.textureFiltering(), TextureFilteringMethod.NONE);
            break;
         case FANCY:
            int j = 16;
            this.set(optionssubscreen, p_456789_.options.biomeBlendRadius(), 2);
            this.set(optionssubscreen, p_456789_.options.renderDistance(), 16);
            this.set(optionssubscreen, p_456789_.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
            this.set(optionssubscreen, p_456789_.options.simulationDistance(), 12);
            this.set(optionssubscreen, p_456789_.options.ambientOcclusion(), true);
            this.set(optionssubscreen, p_456789_.options.cloudStatus(), CloudStatus.FANCY);
            this.set(optionssubscreen, p_456789_.options.particles(), ParticleStatus.ALL);
            this.set(optionssubscreen, p_456789_.options.mipmapLevels(), 4);
            this.set(optionssubscreen, p_456789_.options.entityShadows(), true);
            this.set(optionssubscreen, p_456789_.options.entityDistanceScaling(), 1.0);
            this.set(optionssubscreen, p_456789_.options.menuBackgroundBlurriness(), 5);
            this.set(optionssubscreen, p_456789_.options.cloudRange(), 64);
            this.set(optionssubscreen, p_456789_.options.cutoutLeaves(), true);
            this.set(optionssubscreen, p_456789_.options.improvedTransparency(), false);
            this.set(optionssubscreen, p_456789_.options.weatherRadius(), 10);
            this.set(optionssubscreen, p_456789_.options.maxAnisotropyBit(), 1);
            this.set(optionssubscreen, p_456789_.options.textureFiltering(), TextureFilteringMethod.RGSS);
            break;
         case FABULOUS:
            int i = 32;
            this.set(optionssubscreen, p_456789_.options.biomeBlendRadius(), 2);
            this.set(optionssubscreen, p_456789_.options.renderDistance(), 32);
            this.set(optionssubscreen, p_456789_.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
            this.set(optionssubscreen, p_456789_.options.simulationDistance(), 12);
            this.set(optionssubscreen, p_456789_.options.ambientOcclusion(), true);
            this.set(optionssubscreen, p_456789_.options.cloudStatus(), CloudStatus.FANCY);
            this.set(optionssubscreen, p_456789_.options.particles(), ParticleStatus.ALL);
            this.set(optionssubscreen, p_456789_.options.mipmapLevels(), 4);
            this.set(optionssubscreen, p_456789_.options.entityShadows(), true);
            this.set(optionssubscreen, p_456789_.options.entityDistanceScaling(), 1.25);
            this.set(optionssubscreen, p_456789_.options.menuBackgroundBlurriness(), 5);
            this.set(optionssubscreen, p_456789_.options.cloudRange(), 128);
            this.set(optionssubscreen, p_456789_.options.cutoutLeaves(), true);
            this.set(optionssubscreen, p_456789_.options.improvedTransparency(), Util.getPlatform() != Util.OS.OSX);
            this.set(optionssubscreen, p_456789_.options.weatherRadius(), 10);
            this.set(optionssubscreen, p_456789_.options.maxAnisotropyBit(), 2);
            if (GraphicsWorkarounds.get(gpudevice).isAmd()) {
               this.set(optionssubscreen, p_456789_.options.textureFiltering(), TextureFilteringMethod.RGSS);
            } else {
               this.set(optionssubscreen, p_456789_.options.textureFiltering(), TextureFilteringMethod.ANISOTROPIC);
            }
      }
   }

   <T> void set(@Nullable OptionsSubScreen p_451738_, OptionInstance<T> p_452148_, T p_456906_) {
      if (p_452148_.get() != p_456906_) {
         p_452148_.set(p_456906_);
         if (p_451738_ != null) {
            p_451738_.resetOption(p_452148_);
         }
      }
   }
}
