package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;

public class IronGolemCrackinessLayer extends RenderLayer<IronGolemRenderState, IronGolemModel> {
   private static final Map<Crackiness.Level, Identifier> identifiers = ImmutableMap.of(
      Crackiness.Level.LOW,
      Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem_crackiness_low.png"),
      Crackiness.Level.MEDIUM,
      Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem_crackiness_medium.png"),
      Crackiness.Level.HIGH,
      Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem_crackiness_high.png")
   );

   public IronGolemCrackinessLayer(final RenderLayerParent<IronGolemRenderState, IronGolemModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final IronGolemRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (!state.isInvisible) {
         Crackiness.Level crackiness = state.crackiness;
         if (crackiness != Crackiness.Level.NONE) {
            Identifier damageTexture = identifiers.get(crackiness);
            renderColoredCutoutModel(this.getParentModel(), damageTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
         }
      }
   }
}
