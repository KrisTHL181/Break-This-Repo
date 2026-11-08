package net.minecraft.client.renderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record PanoramicScreenshotParameters(Vector3fc forwardVector) {
}
