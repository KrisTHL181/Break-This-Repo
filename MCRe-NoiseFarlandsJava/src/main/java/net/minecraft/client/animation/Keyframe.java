package net.minecraft.client.animation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record Keyframe(float timestamp, Vector3fc preTarget, Vector3fc postTarget, AnimationChannel.Interpolation interpolation) {
    public Keyframe(final float timestamp, final Vector3fc postTarget, final AnimationChannel.Interpolation interpolation) {
        this(timestamp, postTarget, postTarget, interpolation);
    }
}