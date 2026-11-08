package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@FunctionalInterface
public interface BlockQuadOutput {
   void put(float x, float y, float z, BakedQuad quad, QuadInstance instance);
}
