package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyDrownedModel;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public class DrownedOuterLayer extends RenderLayer<ZombieRenderState, DrownedModel> {
   private static final Identifier DROWNED_OUTER_LAYER_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png");
   private static final Identifier BABY_DROWNED_OUTER_LAYER_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer_baby.png");
   private final DrownedModel model;
   private final DrownedModel babyModel;

   public DrownedOuterLayer(final RenderLayerParent<ZombieRenderState, DrownedModel> renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.model = new DrownedModel(modelSet.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
      this.babyModel = new BabyDrownedModel(modelSet.bakeLayer(ModelLayers.DROWNED_BABY_OUTER_LAYER));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final ZombieRenderState state,
      final float yRot,
      final float xRot
   ) {
      DrownedModel model = state.isBaby ? this.babyModel : this.model;
      Identifier layerLocation = state.isBaby ? BABY_DROWNED_OUTER_LAYER_LOCATION : DROWNED_OUTER_LAYER_LOCATION;
      coloredCutoutModelCopyLayerRender(model, layerLocation, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
   }
}
