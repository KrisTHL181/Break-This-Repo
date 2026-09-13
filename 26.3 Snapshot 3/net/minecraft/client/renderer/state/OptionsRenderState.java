package net.minecraft.client.renderer.state;

import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;

public class OptionsRenderState {
   public int cloudRange;
   public boolean cutoutLeaves;
   public boolean improvedTransparency;
   public boolean ambientOcclusion;
   public int menuBackgroundBlurriness;
   public double panoramaSpeed;
   public int maxAnisotropyValue;
   public TextureFilteringMethod textureFiltering = TextureFilteringMethod.NONE;
   public boolean bobView;
   public float screenEffectScale;
   public double glintSpeed;
   public double glintStrength;
   public double damageTiltStrength;
   public boolean backgroundForChatOnly;
   public float textBackgroundOpacity;
   public CloudStatus cloudStatus = CloudStatus.OFF;
   public CameraType cameraType = CameraType.FIRST_PERSON;
   public int renderDistance;
   public double chunkSectionFadeInTime;
   public PrioritizeChunkUpdates prioritizeChunkUpdates;
   public int fov;

   public float getBackgroundOpacity(final float defaultOpacity) {
      return this.backgroundForChatOnly ? defaultOpacity : this.textBackgroundOpacity;
   }
}
