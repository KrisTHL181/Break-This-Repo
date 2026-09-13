package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class SkullModelBase extends Model<SkullModelBase.State> {
   public SkullModelBase(ModelPart p_451762_) {
      super(p_451762_, RenderTypes::entityTranslucent);
   }

   @OnlyIn(Dist.CLIENT)
   public static class State {
      public float animationPos;
      public float yRot;
      public float xRot;
   }
}
