package net.minecraft.client.resources.model;

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
      return Transformation.identity();
   }

   default Matrix4fc faceTransformation(Direction p_392009_) {
      return NO_TRANSFORM;
   }

   default Matrix4fc inverseFaceTransformation(Direction p_397114_) {
      return NO_TRANSFORM;
   }
}
