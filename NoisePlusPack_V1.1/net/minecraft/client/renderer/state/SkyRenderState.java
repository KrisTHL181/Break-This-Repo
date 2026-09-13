package net.minecraft.client.renderer.state;

import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkyRenderState {
   public DimensionType.Skybox skybox = DimensionType.Skybox.NONE;
   public boolean shouldRenderDarkDisc;
   public float sunAngle;
   public float moonAngle;
   public float starAngle;
   public float rainBrightness;
   public float starBrightness;
   public int sunriseAndSunsetColor;
   public MoonPhase moonPhase = MoonPhase.FULL_MOON;
   public int skyColor;
   public float endFlashIntensity;
   public float endFlashXAngle;
   public float endFlashYAngle;

   public void reset() {
      this.skybox = DimensionType.Skybox.NONE;
   }
}
