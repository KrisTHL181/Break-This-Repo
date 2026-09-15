package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface HeadedModel {
   ModelPart getHead();

   default void translateToHead(PoseStack p_423116_) {
      this.getHead().translateAndRotate(p_423116_);
   }
}
