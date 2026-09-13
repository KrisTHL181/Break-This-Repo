package net.minecraft.client.renderer.state.level;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ParticleGroupRenderState {
    void submit(SubmitNodeCollector submitNodeCollector, final CameraRenderState camera);

    default void clear() {
    }
}