package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public abstract class SkullModelBase extends Model<SkullModelBase.State> {
   public SkullModelBase(final ModelPart root) {
      super(root, RenderTypes::entityTranslucent);
   }

   public static class State {
      public float animationPos;
      public float yRot;
      public float xRot;
   }
}
