package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

public class ZombieModel<S extends ZombieRenderState> extends AbstractZombieModel<S> {
   public ZombieModel(final ModelPart root) {
      super(root);
   }
}
