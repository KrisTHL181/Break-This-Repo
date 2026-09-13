package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker {
   ResolvedModel getModel(Identifier var1);

   BlockModelPart missingBlockModelPart();

   SpriteGetter sprites();

   ModelBaker.PartCache parts();

   <T> T compute(ModelBaker.SharedOperationKey<T> var1);

   @OnlyIn(Dist.CLIENT)
   interface PartCache {
      default Vector3fc vector(float p_452065_, float p_451254_, float p_452365_) {
         return this.vector(new Vector3f(p_452065_, p_451254_, p_452365_));
      }

      Vector3fc vector(Vector3fc var1);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface SharedOperationKey<T> {
      T compute(ModelBaker var1);
   }
}
