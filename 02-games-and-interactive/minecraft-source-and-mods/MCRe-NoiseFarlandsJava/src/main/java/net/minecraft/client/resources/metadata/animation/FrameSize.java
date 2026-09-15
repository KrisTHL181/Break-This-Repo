package net.minecraft.client.resources.metadata.animation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record FrameSize(int width, int height) {
}