package net.minecraft.client.renderer.state;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ParticleGroupRenderState {
   void submit(SubmitNodeCollector var1, CameraRenderState var2);

   default void clear() {
   }
}
