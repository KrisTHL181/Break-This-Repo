package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public class BabyZombifiedPiglinModel extends ZombifiedPiglinModel {
   public BabyZombifiedPiglinModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      return BabyPiglinModel.createBodyLayer();
   }

   @Override
   protected float getDefaultEarAngleInDegrees() {
      return 5.0F;
   }
}
