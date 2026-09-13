package net.minecraft.client.renderer.fog;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FogData {
   public float environmentalStart;
   public float renderDistanceStart;
   public float environmentalEnd;
   public float renderDistanceEnd;
   public float skyEnd;
   public float cloudEnd;
}
