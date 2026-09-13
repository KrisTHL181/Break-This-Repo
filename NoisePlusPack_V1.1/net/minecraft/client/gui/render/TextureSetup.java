package net.minecraft.client.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TextureSetup(
   @Nullable GpuTextureView texure0,
   @Nullable GpuTextureView texure1,
   @Nullable GpuTextureView texure2,
   @Nullable GpuSampler sampler0,
   @Nullable GpuSampler sampler1,
   @Nullable GpuSampler sampler2
) {
   private static final TextureSetup NO_TEXTURE_SETUP = new TextureSetup(null, null, null, null, null, null);
   private static int sortKeySeed;

   public static TextureSetup singleTexture(GpuTextureView p_409143_, GpuSampler p_459065_) {
      return new TextureSetup(p_409143_, null, null, p_459065_, null, null);
   }

   public static TextureSetup singleTextureWithLightmap(GpuTextureView p_409588_, GpuSampler p_458486_) {
      return new TextureSetup(
         p_409588_,
         null,
         Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(),
         p_458486_,
         null,
         RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
      );
   }

   public static TextureSetup doubleTexture(GpuTextureView p_405803_, GpuSampler p_457224_, GpuTextureView p_409777_, GpuSampler p_457113_) {
      return new TextureSetup(p_405803_, p_409777_, null, p_457224_, p_457113_, null);
   }

   public static TextureSetup noTexture() {
      return NO_TEXTURE_SETUP;
   }

   public int getSortKey() {
      return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? this.hashCode() * (sortKeySeed + 1) : this.hashCode();
   }

   public static void updateSortKeySeed() {
      sortKeySeed = Math.round(100000.0F * (float)Math.random());
   }
}
