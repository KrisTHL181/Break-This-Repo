package net.minecraft.client.renderer.block.dispatch;

import com.mojang.math.Transformation;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public interface ModelState {
    Matrix4fc NO_TRANSFORM = new Matrix4f();

    default Transformation transformation() {
        return Transformation.IDENTITY;
    }

    default Matrix4fc faceTransformation(final Direction face) {
        return NO_TRANSFORM;
    }

    default Matrix4fc inverseFaceTransformation(final Direction face) {
        return NO_TRANSFORM;
    }
}